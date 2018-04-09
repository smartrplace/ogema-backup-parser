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

import java.util.List;

import org.ogema.serialization.jaxb.Resource;
import org.smartrplace.analysis.backup.parser.SubresourceUtils;

import de.iwes.widgets.html.tree.NodeDO;

class ResourceTree {

	final List<Resource> resources;
	final NodeDO<Object> root;
	
	public ResourceTree(List<Resource> resources) {
		this.resources = resources;
		if (resources == null || resources.isEmpty())
			this.root = null;
		else if (resources.size() == 1) {
			Resource res = resources.get(0);
			this.root = new NodeDO<>(resources.get(0).getPath(),res);
			parseResources(root, resources.get(0).getSubresources());
		}
		else {
			this.root = new NodeDO<>("/", null); // the top resource "/"
			parseResources(root, resources);
		}
	}
	
	private final void parseResources(NodeDO<Object> node, List<?> resources) {
		for (Object o : resources) {
			// TODO add path?
			NodeDO<Object> n = new NodeDO<>(SubresourceUtils.parseName(o),o);
			node.add(n);
			if (o instanceof Resource) {
				parseResources(n, ((Resource) o).getSubresources());
			}
		}
	}
	
}
