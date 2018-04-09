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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.ogema.recordeddata.DataRecorder;
import org.ogema.serialization.jaxb.Resource;

/**
 * A gateway that is known from its resource backup files and/or log data backups. 
 */
public interface MemoryGateway {

	/**
	 * The folder name.
	 * @return
	 */
	String getId();
	
	/**
	 * Get all toplevel resources
	 * @return
	 */
	Optional<List<Resource>> getAllResources();
	
	/**
	 * Does not return references
	 * @param path
	 * @return
	 */
	Optional<Resource> getResource(String path);
	
	/**
	 * If present, this returns an instance of 
	 * org.smartrplace.logging.fendodb.CloseableDataRecorder
	 * However, to avoid a hard dependency to the FendoDB bundle,
	 * the API only specifies the DataRecorder type
	 * @return
	 */
	Optional<DataRecorder> getLogdata();
	Optional<Map<String,Resource>> getAllRooms();
	Optional<Map<String,Resource>> getAllDevices();
	// TODO check: does this only return toplevel devices?
	Optional<List<Resource>> getDevicesByRoom(Resource room);
//	Optional<ResourceTree> getResourceTree();
	
	
}
