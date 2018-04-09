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
