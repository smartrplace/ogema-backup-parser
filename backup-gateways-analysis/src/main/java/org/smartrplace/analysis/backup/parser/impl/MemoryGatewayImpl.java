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
package org.smartrplace.analysis.backup.parser.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.ogema.model.actors.RemoteControl;
import org.ogema.model.devices.buildingtechnology.AirConditioner;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.devices.generators.PVPlant;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.devices.sensoractordevices.SingleSwitchBox;
import org.ogema.model.devices.storage.ChargingPoint;
import org.ogema.model.devices.storage.ElectricityChargingStation;
import org.ogema.model.locations.Room;
import org.ogema.model.metering.special.FlowProbe;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.DoorWindowSensor;
import org.ogema.model.sensors.HumiditySensor;
import org.ogema.model.sensors.OccupancySensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.recordeddata.DataRecorder;
import org.ogema.serialization.jaxb.Resource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;
import org.smartrplace.analysis.backup.parser.api.MemoryGateway;
import org.smartrplace.analysis.backup.parser.util.MemoryResourceUtilSimple;
import org.smartrplace.analysis.backup.parserv2.BackupParser;
import org.smartrplace.analysis.backup.parserv2.MemoryResourceUtil;
import org.smartrplace.analysis.backup.parserv2.TreeAnalyzer;
import org.smartrplace.analysis.backup.parserv2.TreeAnalyzer.RoomAnalyzer;

/**
 * A gateway that is known from its resource backup files and/or log data backups. 
 *
 */
class MemoryGatewayImpl implements MemoryGateway {

	public final String id;
	private final CountDownLatch latch = new CountDownLatch(1);
	// note: these are not all resources, only the top-level ones from different files; quasi-final
	private List<Resource> resources;
//	private final CloseableDataRecorder logData;
	private final BundleContext ctx;
	private final Path basePath;
	private volatile Map<String,Resource> rooms = null; // cache, populated upon first access
	private volatile Map<String,Resource> installAppDevices = null; // cache, populated upon first access
	private volatile Map<String,Resource> devices = null; // cache
	private volatile Map<String, String> roomsByDevices = null; // cache
	private volatile Map<String, String> installAppByDevices = null; // cache
	
	MemoryGatewayImpl(String id, Path basePath, BackupParser parser, BundleContext ctx) throws IOException, UncheckedIOException {
		this.id = id;
		this.basePath = basePath;
//		CloseableDataRecorder logData = null;
//		try {
//			logData = slots.getInstance(basePath.resolve("slotsdb"));
//		} catch (Exception ignore) {} // folder does not exist
//		this.logData = logData;
		this.ctx = ctx;
		initResources(basePath, parser);
	}
	
	private final void initResources(Path basePath, BackupParser parser) {
		final Runnable run = new Runnable() {
			
			@Override
			public void run() {
				try {
					List<Resource> r;
					try {
						final Optional<Path> opt = getLatestBackup(basePath);
						if (!opt.isPresent()) {
							resources = null;
							return;
						}
						Path latestBackup = opt.get();
						r = Collections.unmodifiableList(parser.parseFolder(latestBackup,false));
//					if (logData == null && r == null)
//						throw new IOException("Gateway " + id + " not found");
						if (r == null && !Files.exists(basePath.resolve("slotsdb")))
							throw new IOException("Gateway " + id + " not found");
					} catch (NoSuchElementException | IOException | UncheckedIOException e) {
						LoggerFactory.getLogger(MemoryGatewayImpl.class).error("Parsing gateway failed.", e);
						r = null;
					}
					resources = r;
				} finally {
					latch.countDown();
				}
			}
		};
		final Thread thread = new Thread(run, "MemoryGateway_init_" + id);
		thread.start();
		
	}
	
	private List<Resource> getResources() {
		try {
			latch.await();
		} catch (InterruptedException e) {
			return null;
		}
		return resources;
	}
	
	@Override
	public String getId() {
		return id;
	}
	
	@Override
	public Optional<List<Resource>> getAllResources() {
		final List<Resource> resources = getResources();
		return resources != null ? Optional.of(resources) : Optional.empty();
	}
	
	@Override
	public Optional<Resource> getResource(String path) {
		final Optional<List<Resource>> opt = getAllResources();
		if (!opt.isPresent())
			return Optional.empty();
		final String[] arr = path.split("/");
		List<?> resources = opt.get();
		Resource target = null;
		for (int i=0;i<arr.length;i++) {
			final String name = arr[i];
			final Optional<Resource> optRes = resources.stream()
				.filter(r -> r instanceof Resource)
				.map(r -> (Resource) r)
				.filter(r -> r.getName().equals(name))
				.findAny();
			if (!optRes.isPresent())
				return Optional.empty();
			target = optRes.get();
			resources = target.getSubresources();
		}
		if (target == null)
			return Optional.empty();
		return Optional.of(target);
	}
	
	@Override
	public Optional<DataRecorder> getLogdata() {
		try {
			final ServiceReference<?> ref = ctx.getServiceReference(org.smartrplace.logging.fendodb.FendoDbFactory.class);
			if (ref == null)
				return Optional.empty();
			final org.smartrplace.logging.fendodb.FendoDbFactory factory = (org.smartrplace.logging.fendodb.FendoDbFactory) ctx.getService(ref);
			return Optional.of(factory.getInstance(basePath.resolve("slotsdb"), org.smartrplace.logging.fendodb.FendoDbConfigurationBuilder.getInstance()
					.setParseFoldersOnInit(true)
					.setReadOnlyMode(true)
					.build()));
		} catch (NoClassDefFoundError | IOException e) {
			return Optional.empty();
		}
	}
	
	@Override
	public Optional<Map<String,Resource>> getAllRooms() {
		if (rooms != null) 
			return Optional.of(rooms);
		final List<Resource> resources = getResources();
		if (resources == null)
			return Optional.empty();
		synchronized (resources) {
			if (rooms != null) 
				return Optional.of(rooms);
			rooms = Collections.unmodifiableMap(MemoryResourceUtil.findResources(resources, resource -> Room.class.getName().equals(resource.getType()), true));
		}
		return Optional.of(rooms);
	}
	
	@Override
	public Optional<Map<String,Resource>> getAllInstallAppDevices() {
		if (installAppDevices != null) 
			return Optional.of(installAppDevices);
		final List<Resource> resources = getResources();
		if (resources == null)
			return Optional.empty();
		synchronized (resources) {
			if (installAppDevices != null) 
				return Optional.of(installAppDevices);
			installAppDevices = Collections.unmodifiableMap(
					MemoryResourceUtil.findResources(resources, resource -> MemoryResourceUtilSimple.INSTALL_APP_DEVICE_CLASS_NAME.equals(resource.getType()), true));
		}
		return Optional.of(installAppDevices);
	}

	@Override
	public Optional<Map<String,Resource>> getAllDevices() {
		if (devices != null) 
			return Optional.of(devices);
		final List<Resource> resources = getResources();
		if (resources == null)
			return Optional.empty();
		synchronized (resources) {
			if (devices != null) 
				return Optional.of(devices);
			if(Boolean.getBoolean("org.smartrplace.analysis.backup.parser.addnetworkTrafficData")) {
				Map<String, Resource> modmap = MemoryResourceUtil.findResources(resources, resource -> 
						deviceTypeNames.stream().collect(Collectors.toList()).contains(resource.getType()), true);
				Map<String, Resource> addmap = Collections.unmodifiableMap(MemoryResourceUtil.findResources(resources, resource -> 
					deviceNetworkNames.stream().collect(Collectors.toList()).contains(resource.getName()), true));
				for(Entry<String, Resource> e: addmap.entrySet()) {
					modmap.put(e.getKey(), e.getValue());
				}
				devices = Collections.unmodifiableMap(modmap);
			} else
				devices = Collections.unmodifiableMap(MemoryResourceUtil.findResources(resources, resource -> 
					deviceTypeNames.stream().collect(Collectors.toList()).contains(resource.getType()), true));
					//deviceTypes.stream().map(clzz -> clzz.getName()).collect(Collectors.toList()).contains(resource.getType()), true));
		}
		return Optional.of(devices);
	}
	
	@Override
	public Optional<List<Resource>> getDevicesByRoom(Resource room) {
		final List<Resource> resources = getResources();
		if (resources == null)
			return Optional.empty();
		if (roomsByDevices == null) {
			synchronized (resources) {
				if (roomsByDevices == null) {
					Collection<Resource> dev = getAllDevices().get().values();
					RoomAnalyzer ra = new RoomAnalyzer(false);
					for (Resource res : dev) {
						ra.parse(res);
					} 
					roomsByDevices = ra.getMap();
				}
			}
		}
		final String path  = room.getPath();
		List<Resource> devices = new ArrayList<>();
		for (Map.Entry<String, String> entry: roomsByDevices.entrySet()) {
			if (path.equals(entry.getValue())) {
				Resource res = getAllDevices().get().get(entry.getKey());
				if (res != null) {
					devices.add(res);
				}
			}
		}
		return Optional.of(devices);
	}
	
	// XXX TODO
	private static final List<Class<? extends org.ogema.core.model.Resource>> deviceTypes = 
			Arrays.asList(PhysicalElement.class, Thermostat.class, TemperatureSensor.class,
					HumiditySensor.class, OccupancySensor.class, SensorDevice.class, 
					SingleSwitchBox.class, RemoteControl.class, DoorWindowSensor.class,
					AirConditioner.class, ElectricityConnectionBox.class,
					ElectricityChargingStation.class, PVPlant.class, ChargingPoint.class, FlowProbe.class);
	private static final List<String> deviceTypeNames = new ArrayList<>();
	private static final List<String> deviceNetworkNames = new ArrayList<>();
	static {
		for(Class<? extends org.ogema.core.model.Resource> type: deviceTypes) {
			deviceTypeNames.add(type.getName());
			deviceTypeNames.add("de.iwes.ogema.bacnet.models.BACnetDevice");
			deviceTypeNames.add("org.smartrplace.iotawatt.ogema.resources.IotaWattElectricityConnection");
			deviceTypeNames.add("org.smartrplace.iotawatt.ogema.resources.IotaWattConnection");
			
			deviceNetworkNames.add("Gateway_Device");
			deviceNetworkNames.add("NetworkTrafficData");
		}
	}
	
	private static Optional<Path> getLatestBackup(Path base) throws IOException {
		return Files.list(base).filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".zip"))
			.max(new Comparator<Path>() {

				@Override
				public int compare(Path o1, Path o2) {
					String f1 = o1.getFileName().toString();
					String f2 = o2.getFileName().toString();
					int[] t1 = getTimestamp(f1);
					int[] t2 = getTimestamp(f2);
					if (t1 == null || t2 == null) {
						if (t1 == null) {
							if (t2 == null)
								return 0;
							return -1;
						}
						return 1;
					}
					int diff;
					for (int i = 0;i<6;i++) {
						diff = t1[i]-t2[i];
						if (diff != 0)
							return diff;
					}
					return 0;
				}
		});
	}
	
	private static final int[] getTimestamp(String filename) {
		if (filename.startsWith("logdata"))
			return null;
		int[] yearMonthDayHourMinSec = new int[6];
		String[] components = filename.split("-");
		int sz = components.length; 
		if (sz < 6) {
			return null;
		}
		String c;
		int idx;
		for (int i=0;i<6;i++) {
			c = components[sz-1-i];
			if (i == 0)
				c = c.toLowerCase().replace(".zip", "");
			else if (i == 5) {
				int length = c.length();
				char ch;
				int cnt = 0;
				boolean digit;
				StringBuilder sb= new StringBuilder();
				do {
					ch = c.charAt(length-1-cnt++);
					digit = Character.isDigit(ch);
					if (digit)
						sb.append(ch);
				} while (cnt < length && digit);
				c = sb.reverse().toString();
			}
			try {
//				idx = (i<3) ? (5-i) : (i-3); // old version
				idx = 5-i;
				yearMonthDayHourMinSec[idx] = Integer.parseInt(c); 
			} catch (NumberFormatException e) {
				return null;
			}
		}
		// this can happen e.g. if the file name format does not obey the convention yyy-mm-dd-HH-MM-SS
		if (yearMonthDayHourMinSec[1] > 12 || yearMonthDayHourMinSec[2] > 31)
			return null;
		return yearMonthDayHourMinSec;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof MemoryGatewayImpl))
			return false;
		return id.equals(((MemoryGatewayImpl) obj).id);
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
}
