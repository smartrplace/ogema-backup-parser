/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.analysis.backup.viz.impl;

import java.util.List;

import org.ogema.serialization.jaxb.Resource;
import org.smartrplace.analysis.backup.parserv2.SubresourceUtils;

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
