package org.smartrplace.backup.parser.impl;

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
