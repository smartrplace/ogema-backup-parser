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
package org.smartrplace.analysis.backup.parserv2.impl;

import org.ogema.serialization.jaxb.Resource;
import org.ogema.serialization.jaxb.ResourceLink;

public class ResourceLinkImpl extends ResourceLink {
	
	public ResourceLinkImpl(Resource resource) {
		super.setType(resource.getType());
		super.setName(resource.getName());
		super.setLink(resource.getPath());
	}

	@Override
	public void setLink(String value) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setType(String value) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setName(String value) {
		throw new UnsupportedOperationException();
	}
	
}
