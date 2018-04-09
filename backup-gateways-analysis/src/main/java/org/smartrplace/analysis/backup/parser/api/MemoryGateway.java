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
