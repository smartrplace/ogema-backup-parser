package org.smartrplace.backup.parser;

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
