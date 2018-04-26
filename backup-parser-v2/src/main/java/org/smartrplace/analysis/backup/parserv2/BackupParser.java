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
