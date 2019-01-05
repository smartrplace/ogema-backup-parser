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
