package org.smartrplace.analysis.backup.viz.impl;

import java.util.List;

import org.ogema.serialization.jaxb.Resource;
import org.smartrplace.backup.parser.SubresourceUtils;

import de.iwes.widgets.html.tree.NodeDO;

class ResourceTree {

	final List<Resource> resources;
	final NodeDO<Object> root;
	
	ResourceTree(List<Resource> resources) {
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
	
	private static void parseResources(NodeDO<Object> node, List<?> resources) {
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
