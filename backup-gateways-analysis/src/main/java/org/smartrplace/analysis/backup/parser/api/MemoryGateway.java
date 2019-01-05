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
