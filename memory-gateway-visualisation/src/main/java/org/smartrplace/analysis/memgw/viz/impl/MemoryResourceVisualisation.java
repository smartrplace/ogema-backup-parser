/**
 * Copyright 2018 Smartrplace UG
 *
 * The OGEMA backup parser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The OGEMA backup parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.smartrplace.analysis.memgw.viz.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.serialization.jaxb.Resource;
import org.ogema.serialization.jaxb.ResourceLink;
import org.smartrplace.analysis.backup.parser.BackupParser;
import org.smartrplace.analysis.backup.parser.MemoryResourceUtil;
import org.smartrplace.analysis.backup.parser.SubresourceUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.textfield.TextField;
import de.iwes.widgets.html.tree.Tree;

class MemoryResourceVisualisation {
	
	private final WidgetPage<?> page;
	private final Header header;
	private final Alert alert;
	private final TextField folderSelector;
	private final Button submit;
	private final Tree<Object> tree; // type can be Resource or ResourceLink, therefore we need to use object as generic argument
	private final Cache<Path, ResourceTree> cache = CacheBuilder.newBuilder().softValues().build(); 
	
	MemoryResourceVisualisation(final WidgetPage<?> page, final BackupParser parser) {
		
		this.page = page;
		 header = new Header(page, "header", "Memory Resource viewer");
		 header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_CENTERED);
		 alert = new Alert(page, "alert", "");
		 alert.setDefaultVisibility(false);
		 folderSelector = new TextField(page, "folderSelector");
		 tree = new Tree<Object>(page, "tree") {

			private static final long serialVersionUID = 1L;
			 
			@Override
			protected Map<String, String> getObjectRepresentation(Object object) {
				Map<String,String> props = new HashMap<>();
				props.put("Name", SubresourceUtils.parseName(object));
				props.put("Path", SubresourceUtils.parsePath(object));
				props.put("Type", SubresourceUtils.parseType(object));
				props.put("Reference", String.valueOf(object instanceof ResourceLink));
				if (object instanceof ResourceLink) 
					props.put("Link", ((ResourceLink) object).getLink());
				if (object instanceof Resource) {
					String value = MemoryResourceUtil.getValue((Resource) object);
					if (value != null)
						props.put("Value", value);
				}
				return props;
			}
			 
		 };
		 submit = new Button(page, "submi", "Load files") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				String value = folderSelector.getValue(req);
				Path path = Paths.get(value);
				ResourceTree tree = null;
				List<Resource> resources = null;
				for (Path p : cache.asMap().keySet()) {
					try {
						if (Files.isSameFile(p, path)) {
							tree = cache.getIfPresent(p);
							resources = tree.resources;
							break;
						}
					} catch (IOException e) {
						if (p.equals(path)) {
							tree = cache.getIfPresent(p);
							resources = tree.resources;
							break;
						}
					}
				}
				if (resources == null) {
					if (Files.isDirectory(path)) {
						try {
							resources = parser.parseFolder(path, true);
						} catch (IOException e) {
							alert.showAlert("Parsing directory failed: "+ path + ": " + e, false, req);
							return;
						}
					}
					else if (Files.isRegularFile(path)) {
						try {
							if (path.toString().toLowerCase().endsWith(".zip")) 
								resources = parser.parseFolder(path, true);
							else
								resources = Collections.singletonList(parser.parse(path));
						} catch (IOException e) {
							alert.showAlert("Parsing file failed: "+ path + ": " + e, false, req);
							return;
						}
					}
					else {
						alert.showAlert("Invalid file " + path + "; exists: " + Files.exists(path), false, req);
						return;
					}
					tree = new ResourceTree(resources);
					cache.put(path, tree);
				}
				MemoryResourceVisualisation.this.tree.setTreeRoot(tree.root, req);
			}
			 
		 };

		buildPage();
		setDependencies(); 
	}

	private final void buildPage() {
		page.append(header).linebreak().append(alert);
		StaticTable table = new StaticTable(2, 2, new int[]{2,3});
		table.setContent(0, 0, "Select a folder/file").setContent(0, 1, folderSelector)
			.setContent(1, 1, submit);
		page.append(table).linebreak().append(tree);
	}
	
	private final void setDependencies() {
		submit.triggerAction(tree, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		submit.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
	
}
