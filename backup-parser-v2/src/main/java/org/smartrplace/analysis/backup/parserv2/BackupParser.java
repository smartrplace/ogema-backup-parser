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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Parses Resources in xml representation. Based on the OGEMA serialization manager. 
 */
public interface BackupParser {

	/**
	 * @param file
	 * 		An XML file.
	 * @return
	 * @throws IOException
	 */
	org.ogema.serialization.jaxb.Resource parse(Path file) throws IOException;
	
	org.ogema.serialization.jaxb.Resource parse(InputStream stream) throws IOException;
	org.ogema.serialization.jaxb.Resource parse(String xml) throws IOException;

	/**
	 * Parse a folder for xml files. Only files ending on ".xml" or ".ogx" are parsed.
	 * @param folder
	 * 		A directory, or a zip file containing XML files. 
	 * @param recursive
	 * @return
	 * @throws IOException
	 */
	List<org.ogema.serialization.jaxb.Resource> parseFolder(Path folder, boolean recursive) throws IOException;
	
}
