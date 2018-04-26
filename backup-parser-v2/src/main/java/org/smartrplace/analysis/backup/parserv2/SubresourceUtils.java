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
package org.smartrplace.analysis.backup.parserv2;

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
