package org.smartrplace.backup.parser;

import org.ogema.serialization.jaxb.Resource;
import org.ogema.serialization.jaxb.ResourceLink;

public class SubresourceUtils {
	
	public final static String parseName(Object object) {
		if (object instanceof Resource)
			return ((Resource) object).getName();
		if (object instanceof ResourceLink)
			return ((ResourceLink) object).getName();
		throw new IllegalArgumentException("Only Resource and ResourceLink allowed, got " + (object == null ? null : object.getClass().getName()));
	}
	
	public final static String parsePath(Object object) {
		if (object instanceof Resource)
			return ((Resource) object).getPath();
		if (object instanceof ResourceLink)
			return ((ResourceLink) object).getLink();
		throw new IllegalArgumentException("Only Resource and ResourceLink allowed, got " + (object == null ? null : object.getClass().getName()));
	}
	
	public final static String parseType(Object object) {
		if (object instanceof Resource)
			return ((Resource) object).getType();
		if (object instanceof ResourceLink)
			return ((ResourceLink) object).getType();
		throw new IllegalArgumentException("Only Resource and ResourceLink allowed, got " + (object == null ? null : object.getClass().getName()));
	}

}
