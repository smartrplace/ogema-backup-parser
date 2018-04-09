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
package org.smartrplace.analysis.backup.parser.impl;

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
