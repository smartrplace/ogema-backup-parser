package org.smartrplace.analysis.backup.parser.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Parses a folder for gateway resource backup data (a zip file containing xml files of serialized OGEMA resources),
 * and its logdata (a folder names "slotsdb").
 *  
 * The base folder can be set via the system property "org.smartrplace.analysis.backup.parser.basepath", default is
 * "ogemaCollect/rest". Each gateway needs to have its own subfolder, whose name is interpreted as the gateway "id".
 * 
 * It is not required for both log data and backup files to be present.
 */
public interface GatewayBackupAnalysis {
	
	/**
	 * Get the gateway ids, i.e. the subfolder names of the base path.
	 * @return
	 */
	List<String> getGatewayIds();
	
	/**
	 * Does a subfolder with the given name exist?
	 * @param id
	 * @return
	 */
	boolean hasGateway(String id);
	
	/**
	 * @param id
	 * @return
	 * @throws IOException
	 * 		If neither log data nor resource backups are found for the requested gateway, or 
	 * 		an error occured when accessing the files. 
	 * @throws UncheckedIOException
	 */
	MemoryGateway getGateway(String id) throws IOException, UncheckedIOException;

}
