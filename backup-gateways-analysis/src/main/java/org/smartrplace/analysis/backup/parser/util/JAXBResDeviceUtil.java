package org.smartrplace.analysis.backup.parser.util;

import java.util.Map.Entry;

import org.ogema.serialization.jaxb.Resource;
import org.smartrplace.analysis.backup.parser.util.MemoryResourceUtilSimple.MemoryResourcePreAnalysis;

/** Same functionality as GeneralResDeviceUtil, but for normal resources*/
public class JAXBResDeviceUtil {
	/** Get a device that is a super-resource of res that contains room information.
	 *  Get device with room if possible, other device without room. It two results are possible return most top-level device
	 *  as we usually do not want to separate into sub-devices.
	 * @param res
	 * @param preData
	 * @return
	 */
	public static Resource getDeviceWithRoomForResource(Resource res, MemoryResourcePreAnalysis preData) {
		String path = res.getPath();
		Resource result = null;
		for(Entry<String, Resource> e: preData.roomByDevice.entrySet()) {
			if(path.startsWith(e.getKey())) {
				if((path.length() > e.getKey().length()) && (path.charAt(e.getKey().length()-1) != '/') && (path.charAt(e.getKey().length()) != '/'))
					continue;
				if(result == null || result.getPath().length() > e.getKey().length())
					result = preData.allDevices.get(e.getKey());
			}
		}
		if(result != null)
			return result;
		for(Entry<String, Resource> e: preData.allDevices.entrySet()) {
			if(path.startsWith(e.getKey())) {
				if((path.length() > e.getKey().length()) && (path.charAt(e.getKey().length()-1) != '/') && (path.charAt(e.getKey().length()) != '/'))
					continue;
				if(result == null || result.getPath().length() > e.getKey().length())
					result = e.getValue();
			}
		}
		return result;
	}
	public static Resource getRoomForResource(Resource res, MemoryResourcePreAnalysis preData) {
		String path = res.getPath();
		for(Entry<String, Resource> e: preData.roomByDevice.entrySet()) {
			if(path.startsWith(e.getKey()))
				return e.getValue();
		}
		return null;
	}
	

}
