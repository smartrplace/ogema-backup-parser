package org.smartrplace.analysis.backup.parser.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.recordeddata.DataRecorder;
import org.ogema.serialization.jaxb.Resource;
import org.ogema.serialization.jaxb.ResourceLink;
import org.smartrplace.analysis.backup.parser.api.GatewayBackupAnalysis;
import org.smartrplace.analysis.backup.parser.api.MemoryGateway;
import org.smartrplace.analysis.backup.parserv2.MemoryResourceUtil;
import org.smartrplace.analysis.backup.parserv2.SubresourceUtils;

public class MemoryResourceUtilSimple {
	public static final String INSTALL_APP_DEVICE_CLASS_NAME = "org.smartrplace.apps.hw.install.config.InstallAppDevice";
	
	public static class MemoryResourcePreAnalysis {
		//device.path -> device
		public Map<String, Resource> allDevices;
		public Map<String, Resource> allRooms;
		public Map<String, Resource> allInstallAppDevices;
		//Room.path -> devices in room
		public Map<String, List<Resource>> devicesByRooms;
		//device.path -> room of device (devices without room are not in the map)
		public Map<String, Resource> roomByDevice; 
		
		public List<Resource> allResources;
	}
	
	//public static List<Resource> getDevices();
	
	public static MemoryGateway getGateway(GatewayBackupAnalysis gatewayParser, String gwId) {
		try {
			MemoryGateway result = gatewayParser.getGateway(gwId);
			return result;
		} catch (UncheckedIOException | IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static DataRecorder getLogRecorder(MemoryGateway memGw) {
		try {
			Optional<DataRecorder> opt = memGw.getLogdata();
			if(opt.isPresent())
				return opt.get();
			else
				return null;
		} catch (UncheckedIOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static ReadOnlyTimeSeries getTimeseries(MemoryGateway memGw, String resLocation) {
		DataRecorder rec = getLogRecorder(memGw);
		if(rec == null)
			return null;
		return rec.getRecordedDataStorage(resLocation);
	}

	public static Resource getResource(MemoryGateway memGw, String path) {
		Optional<Resource> opt = memGw.getResource(path);
		if(opt.isPresent())
			return opt.get();
		else
			return null;
	}

	public static MemoryResourcePreAnalysis getAllResources(MemoryGateway memGw) {
		MemoryResourcePreAnalysis result = new MemoryResourcePreAnalysis();
		Optional<List<Resource>> opt = memGw.getAllResources();
		if(opt.isPresent())
			result.allResources = opt.get();
		else
			return null;

		Optional<Map<String, Resource>> opt2 = memGw.getAllDevices();
		if(opt2.isPresent())
			result.allDevices = opt2.get();
		else
			result.allDevices = new HashMap<String, Resource>();
		
		Optional<Map<String, Resource>> opt3 = memGw.getAllRooms();
		if(opt3.isPresent())
			result.allRooms = opt3.get();
		else
			result.allRooms = new HashMap<String, Resource>();

		Optional<Map<String, Resource>> opt5 = memGw.getAllInstallAppDevices();
		if(opt5.isPresent())
			result.allInstallAppDevices = opt5.get();
		else
			result.allInstallAppDevices = new HashMap<String, Resource>();

		result.devicesByRooms = new HashMap<String, List<Resource>>();
		result.roomByDevice = new HashMap<String, Resource>();
		for(Resource room: result.allRooms.values()) {
			Optional<List<Resource>> opt4 = memGw.getDevicesByRoom(room);
			if(opt4.isPresent()) {
				List<Resource> devices = opt4.get();
				result.devicesByRooms.put(room.getPath(), devices);
				for(Resource dev: devices) {
					result.roomByDevice.put(dev.getPath(), room);
				}
			}
		}

		return result;
	}
	
	public static Resource getParent(Resource res, MemoryGateway memGw ) {
		String[] els = res.getPath().split("/");
		if(els.length < 2)
			return null;
		String parentPath = els[0];
		for(int i=1; i<els.length-1; i++) {
			parentPath += "/"+els[i];
		}
		return MemoryResourceUtilSimple.getResource(memGw, parentPath);
	}
	
	public static Resource getDeviceResourceByAppInstall(Resource appInstallDevice, MemoryResourcePreAnalysis preData) {
		if(!appInstallDevice.getType().equals(INSTALL_APP_DEVICE_CLASS_NAME))
			throw new IllegalStateException("Wrong type for input:"+appInstallDevice.getType());
		for (Object res : appInstallDevice.getSubresources()) {
			if (res instanceof ResourceLink) { 
				ResourceLink r = (ResourceLink) res;
				if (r.getName().equals("device")) {
					String location = SubresourceUtils.parsePath(r);
					return preData.allDevices.get(location);
				}
			}
		}
		return null;
	}
	
	public static Resource getInstallAppResource(Resource device, MemoryResourcePreAnalysis preData) {
		for(Resource appInstall: preData.allInstallAppDevices.values()) {
			Resource deviceToTest = getDeviceResourceByAppInstall(appInstall, preData);
			if(deviceToTest == null)
				continue;
			if(deviceToTest.getPath().equals(device.getPath()))
				return appInstall;
		}
		return null;
	}
	
	public static String getSubResourceValue(Resource res, String name) {
		Resource valRes = res.get(name);
		return MemoryResourceUtil.getValue(valRes);
	}
	
	/**Like getHumanReadableName, but just return Resource.getName when no other name is specified*/
	public static String getHumanReadableShortName(Resource resource) {
		final String name = getNameResourceValue(resource);
		return name != null ? name : resource.getName();		
	}
	
	/**
	 * Get the trimmed value of the "name" subresource, if it exists, is active, is a StringResource,
	 * and has a non-empty value. Otherwise returns null.
	 * @param resource
	 * @return
	 */
	public static String getNameResourceValue(Resource resource) {
		if (resource == null)
			return null;
		String val = getSubResourceValue(resource, "name");
		if(val == null)
			return null;
		val = val.trim();
		return val.isEmpty() ? null: val;
	}

}
