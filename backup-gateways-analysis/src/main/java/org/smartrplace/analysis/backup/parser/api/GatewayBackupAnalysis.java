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
