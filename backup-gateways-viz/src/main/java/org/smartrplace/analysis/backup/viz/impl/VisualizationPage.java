package org.smartrplace.analysis.backup.viz.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.serialization.jaxb.Resource;
import org.slf4j.LoggerFactory;
import org.smartrplace.analysis.backup.parser.api.GatewayBackupAnalysis;
import org.smartrplace.analysis.backup.parser.api.MemoryGateway;
import org.smartrplace.logging.fendodb.CloseableDataRecorder;
import org.smartrplace.logging.fendodb.FendoTimeSeries;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.accordion.Accordion;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.alert.AlertData;
import de.iwes.widgets.html.calendar.datepicker.Datepicker;
import de.iwes.widgets.html.filedownload.FileDownload;
import de.iwes.widgets.html.filedownload.FileDownloadData;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.checkbox.Checkbox;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;
import de.iwes.widgets.html.multiselect.TemplateMultiselect;
import de.iwes.widgets.html.tree.NodeDO;
import de.iwes.widgets.html.tree.Tree;
import de.iwes.widgets.resource.widget.init.InitUtil;
import de.iwes.widgets.reswidget.scheduleviewer.ScheduleViewerBasic;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfiguration;
import de.iwes.widgets.template.DisplayTemplate;

class VisualizationPage {
	
	private final static String PATH_LOGIC_SEP = "_x_";
	private final static String PATH_RESOURCE_SEP = "_p_";
	private final static String FULL_INTERVAL_DOWNLOAD = "Download full interval";
	
	private final Path temp;
	private final ApplicationManager am;
	private final WidgetPage<?> page;
	private final Header header;
	private final Alert alert;
	private final TemplateDropdown<String> gatewaySelector;
	private final TemplateDropdown<Resource> roomSelector;
//	private final TemplateDropdown<Resource> deviceSelector;
	private final TemplateMultiselect<Resource> deviceSelector;
	private final Accordion dataAccordion; // one tab for resource tree, one for log data, one for export
	private final Tree<Object> resourceTree;
	private final ScheduleViewerBasic<FendoTimeSeries> logDataViewer;
	private final Datepicker downloadStartPicker;
	private final Datepicker downloadEndPicker;
	private final Checkbox completeDataset;
	private final FileDownload logdataDownload;
	private final Button downloadTrigger;
	private final FileDownload fullLogdataDownload;
	private final Button fullDownloadTrigger;
	
	private final GatewayBackupAnalysis controller;
	private final Cache<MemoryGateway, ResourceTree> trees = CacheBuilder.newBuilder().softValues().build();
	
	VisualizationPage(final WidgetPage<?> page, final GatewayBackupAnalysis controller, final Path base, final ApplicationManager am) {
		this.page = page;
		page.showOverlay(true);
		this.am=am;
		this.controller = controller;
		this.temp = am.getDataFile("temp").toPath();
		if (Files.exists(temp)) {
			try {
				Files.list(temp).forEach(path -> {
					try {
						Files.delete(path);
					} catch (IOException ignore) {}
				});
			} catch (Exception ignore) {}
		} else {
			try {
				Files.createDirectories(temp);
			} catch (IOException ignore) {}
		}
		
//		page.showOverlay(true); // XXX fades in and out and in and out... this is annoying
		this.header = new Header(page, "header", "Gateway backup viewer");
		header.addDefaultStyle(HeaderData.CENTERED);
		this.alert = new Alert(page, "alert", "");
		alert.setDefaultVisibility(false);
		this.gatewaySelector = new TemplateDropdown<String>(page, "gatewaySelector") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				
				if (!Files.exists(base) || !Files.isDirectory(base)) {
					alert.setText("Configured directory " + base + " does not exist", req);
					alert.setWidgetVisibility(true, req);
					alert.setStyle(AlertData.BOOTSTRAP_DANGER, req);
					update(Collections.emptyList(),req);
					return;
				}
				update(controller.getGatewayIds(), req);
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				String path = getSelectedItem(req);
				if (path != null) {
					try {
						controller.getGateway(path);
					} catch (IOException | UncheckedIOException e) {
						alert.showAlert("Gateway information for " + path + " could not be read: " + e, false, req);
						return;
					}
				}
				alert.setWidgetVisibility(false, req);
			}
			
		};
//		gatewaySelector.postponeLoading();
		final DisplayTemplate<Resource> resourceTemplate = new DisplayTemplate<Resource>() {

			@Override
			public String getId(Resource res) {
				return res.getPath();
			}

			@Override
			public String getLabel(Resource res, OgemaLocale arg1) {
				return res.getPath();
			}
		};
		this.roomSelector = new TemplateDropdown<Resource>(page, "roomSelector") {

			private static final long serialVersionUID = 1L;

			public void onGET(OgemaHttpRequest req) {
				String path = gatewaySelector.getSelectedItem(req);
				if (path == null) {
					update(Collections.emptyList(),req);
					return;
				}
				
				MemoryGateway gw;
				try {
					gw = controller.getGateway(path);
				} catch (IOException | UncheckedIOException e) {
					update(Collections.emptyList(), req);
					return;
				}
				Collection<Resource> rooms;
				try {
					Map<String, Resource> m = gw.getAllRooms().get();
					rooms = m.values();
				} catch (NoSuchElementException e) {
					rooms = Collections.emptyList();
				}
				update(rooms, req);
			};
			
			
			
		};
		roomSelector.setDefaultAddEmptyOption(true);
		roomSelector.setTemplate(resourceTemplate);
		this.deviceSelector = new TemplateMultiselect<Resource>(page, "deviceSelector") {

			private static final long serialVersionUID = 1L;

			public void onGET(OgemaHttpRequest req) {
				MemoryGateway gw = null;
				try {
					gw = getGateway(req);
				} catch (IOException | UncheckedIOException e) {
				}
				if (gw == null) {
					update(Collections.emptyList(),req);
					return;
				}
				Resource room = roomSelector.getSelectedItem(req);
				Collection<Resource> devices = null;
				if (room == null) {
					try {
						Map<String,Resource> map = gw.getAllDevices().get();
						devices = map.values();
					} catch (NoSuchElementException e) {}
				}
				else 
					devices = gw.getDevicesByRoom(room).get();
				if (devices == null)
					devices = Collections.emptyList();
				update(devices, req);
			};
			
		};
		deviceSelector.setTemplate(resourceTemplate);
		this.dataAccordion = new Accordion(page, "dataAccordion", true);
		this.resourceTree = new Tree<Object>(page, "resourceTree") {

			private static final long serialVersionUID = 1L;
			
			public void onGET(OgemaHttpRequest req) {
				MemoryGateway gw = null;
				try {
					gw = getGateway(req);
				} catch (IOException | UncheckedIOException e) {
				}
				if (gw == null) {
					setTreeRoot(null, req);
					return;
				}
				NodeDO<Object> root;
				try {
					ResourceTree tree = getResourceTree(gw);
					root = tree != null ? tree.root : null;
				} catch (NoSuchElementException e) {
					root = null;
				}
				setTreeRoot(root, req);
			};
			
		};
		DisplayTemplate<FendoTimeSeries> template = new DisplayTemplate<FendoTimeSeries>() {

			@Override
			public String getId(FendoTimeSeries rd) {
				return rd.getPath();
			}

			@Override
			public String getLabel(FendoTimeSeries rd, OgemaLocale locale) {
				try {
					return URLDecoder.decode(rd.getPath(), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					return rd.getPath();
				}
			}
		};
		final ScheduleViewerConfiguration svconfig = new ScheduleViewerConfiguration(false, false, true, false, null, false, 2 *24 * 3600*1000L, 0L, null, null, 24*60*60*1000L);
		this.logDataViewer = new ScheduleViewerBasic<FendoTimeSeries>(page, "scheduleViewer", am, svconfig, template) {

			private static final long serialVersionUID = 1L;
			
			public void onGET(OgemaHttpRequest req) {
				MemoryGateway gw = null;
				try {
					gw = getGateway(req);
				} catch (IOException | UncheckedIOException e) {
				}
				if (gw == null || !gw.getLogdata().isPresent()) {
					setSchedules(Collections.emptyList(), req);
					return;
				}
				Resource room = roomSelector.getSelectedItem(req);
				List<Resource> devices = deviceSelector.getSelectedItems(req);
				if ((devices == null || devices.isEmpty()) && room != null) {
					try {
						devices = gw.getDevicesByRoom(room).get();
					} catch (NoSuchElementException ignore) {}
				}
				try {
					org.smartrplace.logging.fendodb.FendoDbFactory.class.getName();
				} catch (NoClassDefFoundError e) {
					setSchedules(Collections.emptyList(), req);
					return;
				}
				org.smartrplace.logging.fendodb.CloseableDataRecorder slots = null;
				try {
					slots = (org.smartrplace.logging.fendodb.CloseableDataRecorder) gw.getLogdata().get();
				} catch (NoSuchElementException ignore) {}
				List<FendoTimeSeries> logs = slots.getAllTimeSeries();
				final boolean devSelected = (devices != null && !devices.isEmpty());
				if (devSelected) {
					Iterator<FendoTimeSeries> it = logs.iterator();
					boolean found;
					while (it.hasNext()) {
						found = false;
						String id = it.next().getPath();
						for (Resource dev: devices) {
							if (id.startsWith(dev.getPath())) { // FIXME encoding?
								found = true;
								break;
							}
						}
						if (!found)
							it.remove();
					}
				}
				setSchedules(logs, req);
				if (devSelected)
					selectSchedules(logs, req);
				else {
					String[] patterns = InitUtil.getInitParameters(getPage(), req);
					if (patterns == null || patterns.length == 0)
						selectSchedules(Collections.emptyList(), req);
					else {
						String selected = patterns[0];
						for(FendoTimeSeries log: logs) {
							if(log.getPath().equals(selected)) {
								List<FendoTimeSeries> select = new ArrayList<>();
								select.add(log);
								setSchedules(select, req);
							}
						}
					}
				}
				setDownloadInterval(logs, req);
			};
			
		};
		this.downloadStartPicker = new Datepicker(page, "downloadStartPicker");
		this.downloadEndPicker = new Datepicker(page, "downloadEndPicker");
		this.completeDataset = new Checkbox(page, "completeDataset") {

			private static final long serialVersionUID = 1L;

			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				boolean full = getCheckboxList(req).get(FULL_INTERVAL_DOWNLOAD);
				if (full)
					setDownloadInterval(logDataViewer.getSelectedItems(req), req);
			};
			
			public void onGET(OgemaHttpRequest req) {
				boolean full = true;
				final long start = downloadStartPicker.getDateLong(req);
			
				final long end = downloadEndPicker.getDateLong(req);
				SampledValue first;
				SampledValue last;
				for (RecordedData rd: logDataViewer.getSchedules(req)) {
					first = rd.getNextValue(Long.MIN_VALUE);
					last = rd.getPreviousValue(Long.MAX_VALUE);
					if (first == null || last == null)
						continue;
					if (first.getTimestamp() < start || last.getTimestamp() > end) {
						full = false;
						break;
					}
				}
				final Map<String,Boolean> current = getCheckboxList(req);
				current.put(FULL_INTERVAL_DOWNLOAD, full);
				setCheckboxList(current, req);
			};
			
		};
		completeDataset.setDefaultList(Collections.singletonMap(FULL_INTERVAL_DOWNLOAD, false));
		this.logdataDownload = new FileDownload(page, "logdataDownload", am.getWebAccessManager());
		this.downloadTrigger = new Button(page, "downloadTrigger", "Download selected CSV files") {

			private static final long serialVersionUID = 1L;

			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				final Path file;
				try {
					file = generateFile(req);
				} catch (IOException e) {
					alert.showAlert("Could not create zip file, an exception occured: " + e, false, req);
					logdataDownload.disable(req);
					return;
				} 
				if (file != null) {
					logdataDownload.enable(req);
					logdataDownload.setFile(file.toFile(), file.getFileName().toString(), req); 
				} else {
					logdataDownload.disable(req);
				}
				logdataDownload.setDeleteFileAfterDownload(true, req);
			};
			
		};
		this.fullLogdataDownload = new FileDownload(page, "fullLogdataDownload", am.getWebAccessManager());
		this.fullDownloadTrigger = new Button(page, "fullDownloadTrigger", "Download all CSV files", true) {

			private static final long serialVersionUID = 1L;
			private final AtomicLong lastFullDownload = new AtomicLong(0);

			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				if (System.currentTimeMillis() - lastFullDownload.get() < 60000) {
					return;
				}
				synchronized (this) {
					if (System.currentTimeMillis() - lastFullDownload.get() < 60000) {
						return;
					}
					final Path file;
					try {
						file = generateCompleteFile(req);
					} catch (IOException e) {
						alert.showAlert("Could not create zip file, an exception occured: " + e, false, req);
						return;
					} 
					if (file != null) {
						fullLogdataDownload.enable(req);
						fullLogdataDownload.setFile(file.toFile(), file.getFileName().toString(), req); 
						lastFullDownload.set(System.currentTimeMillis());
					} 
				}
			};
			
		};
		
		
		buildPage();
		setDependencies();
	}
	
	private void buildPage() {
		page.append(header).linebreak().append(alert);
		
		StaticTable st = new StaticTable(3, 2, new int[]{3,3});
		st.setContent(0, 0, "Select gateway").setContent(0, 1, gatewaySelector)
			.setContent(1, 0, "Select room").setContent(1, 1, roomSelector)
			.setContent(2, 0, "Select device").setContent(2, 1, deviceSelector);
		page.append(st);
		
//		final Flexbox tab1 = new Flexbox(page, "tab1", true);
//		tab1.addItem(resourceTree, null);
//		final Flexbox tab2 = new Flexbox(page, "tab2", true);
//		tab2.addItem(logDataViewer, null);
//		dataAccordion.addItem("Resource tree", tab1, null);
//		dataAccordion.addItem("Log data", tab2, null);
		dataAccordion.addItem("Resource tree", resourceTree, null);
		dataAccordion.addItem("Log data", logDataViewer, null);
		
		PageSnippet snippet = new PageSnippet(page, "downloadSnippet", true);
		st = new StaticTable(5, 2, new int[]{2,3});
		st.setContent(0,0,"Start time").setContent(0,1,downloadStartPicker)
			.setContent(1, 0, "End time").setContent(1, 1, downloadEndPicker)
			.setContent(2, 0, "Full?").setContent(2, 1, completeDataset)
			.setContent(3, 1, downloadTrigger)
			.setContent(4, 1, fullDownloadTrigger);
		snippet.append(st, null).linebreak(null).append(logdataDownload, null).append(fullLogdataDownload, null);
		dataAccordion.addItem("Export data", snippet, null);
		
		page.append(dataAccordion);
	}
	
	private void setDependencies() {
		gatewaySelector.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		gatewaySelector.triggerAction(roomSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		gatewaySelector.triggerAction(deviceSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST,1);
		gatewaySelector.triggerAction(resourceTree, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST,2);
		gatewaySelector.triggerAction(logDataViewer, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST,2);
		roomSelector.triggerAction(deviceSelector, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		roomSelector.triggerAction(resourceTree, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST,1);
		roomSelector.triggerAction(logDataViewer, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST,1);
		deviceSelector.triggerAction(resourceTree, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		deviceSelector.triggerAction(logDataViewer, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		gatewaySelector.triggerAction(alert,TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		
		logDataViewer.triggerAction(downloadStartPicker, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		logDataViewer.triggerAction(downloadEndPicker, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		logDataViewer.triggerAction(completeDataset, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST, 1);
		completeDataset.triggerAction(downloadStartPicker, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		completeDataset.triggerAction(downloadEndPicker, TriggeringAction.GET_REQUEST, TriggeredAction.GET_REQUEST);
		
		downloadTrigger.triggerAction(logdataDownload, TriggeringAction.POST_REQUEST, FileDownloadData.GET_AND_STARTDOWNLOAD);
		downloadTrigger.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
		fullDownloadTrigger.triggerAction(fullLogdataDownload, TriggeringAction.POST_REQUEST, FileDownloadData.GET_AND_STARTDOWNLOAD);
		fullDownloadTrigger.triggerAction(alert, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
	
	private void setDownloadInterval(List<FendoTimeSeries> schedules, OgemaHttpRequest req) {
		long start = System.currentTimeMillis();
		long end = start;
		SampledValue first;
		SampledValue last;
		for (RecordedData rd: schedules) {
			first = rd.getNextValue(Long.MIN_VALUE);
			if (first == null)
				continue;
			last = rd.getPreviousValue(Long.MAX_VALUE);
			if (last == null) // FIXME this is actually a bug... 
				continue;
			if (first.getTimestamp() < start)
				start = first.getTimestamp();
			if (last.getTimestamp() > end)
				end = last.getTimestamp();
		}
		downloadStartPicker.setDate(start, req);
		downloadEndPicker.setDate(end, req);
	}
	
	private MemoryGateway getGateway(OgemaHttpRequest req) throws IOException, UncheckedIOException  {
		String path = gatewaySelector.getSelectedItem(req);
		if (path == null) {
			return null;
		}
		MemoryGateway gw = controller.getGateway(path);
		return gw;
	}
	
	private String generateDirName(OgemaHttpRequest req) {
		final StringBuilder sb = new StringBuilder();
		String gw = gatewaySelector.getSelectedItem(req);
		if (gw == null)
			return null;
		sb.append("GW_").append(gw);
		Resource res = roomSelector.getSelectedItem(req);
		if (res != null) 
			sb.append(PATH_LOGIC_SEP).append("ROOM_").append(res.getPath().replace("/",PATH_RESOURCE_SEP));
		List<Resource> devs = deviceSelector.getSelectedItems(req);
		if (devs != null && !devs.isEmpty()) {
			if (devs.size() == 1) 
				sb.append(PATH_LOGIC_SEP).append("DEVICE_").append(devs.get(0).getPath().replace("/",PATH_RESOURCE_SEP));
			else
				sb.append(PATH_LOGIC_SEP).append("MULTI_DEVICES");
		}
		return sb.toString();
	}
	
	private Path generateFile(OgemaHttpRequest req) throws IOException {
		String dirName = generateDirName(req);
		if (dirName == null) {
			alert.showAlert("Select a gateway first", false, req);
			return null;
		}
		final Path temp = am.getDataFile("temp").toPath();
		final Path base = temp.resolve(dirName);
		if (Files.exists(base)) {
			if (Files.isDirectory(base))
				FileUtils.deleteDirectory(base.toFile());
			else 
				Files.delete(base);
		}
		Files.createDirectories(base);
		
		final long start = downloadStartPicker.getDateLong(req);
		final long end = downloadEndPicker.getDateLong(req);
		if (start > end) {
			alert.showAlert("Selected start time before end time", false, req);
			return null;
		}
//		final Map<String, String> env = Collections.singletonMap("create", "true");
		final Path zipFile = temp.resolve(dirName + "_" + ((int) (Math.random()*20000)) + ".zip");
        final URI uri = URI.create("jar:" + zipFile.toUri());
		try (final FileSystem zipfs = FileSystems.newFileSystem(uri, Collections.singletonMap("create", "true"))) {
			Path file;
			Path pathInZipfile;
			String filename;
			for (RecordedData rd : logDataViewer.getSchedules(req)) {
				filename = rd.getPath().replace("/", "%2F") + ".csv";
				file = base.resolve(filename);
				toCsvFile(rd.iterator(start,end), file);
	            pathInZipfile = zipfs.getPath("/" + filename);          
	            // copy a file into the zip file
	            Files.copy(file, pathInZipfile, StandardCopyOption.REPLACE_EXISTING );
	            Files.delete(file);
			}
			
		}
		FileUtils.deleteDirectory(base.toAbsolutePath().toFile());
        return zipFile;
	}
	
	private Path generateCompleteFile(OgemaHttpRequest req) throws IOException {
		String dirName = "logdata";
		Path temp = am.getDataFile("temp").toPath();
		final Path base = temp.resolve(dirName);
		if (Files.exists(base)) {
			if (Files.isDirectory(base))
				FileUtils.deleteDirectory(base.toFile());
			else 
				Files.delete(base);
		}
		Files.createDirectories(base);
		
		final long start = downloadStartPicker.getDateLong(req);
		final long end = downloadEndPicker.getDateLong(req);
		if (start > end) {
			alert.showAlert("Selected start time before end time", false, req);
			return null;
		}
//		final Map<String, String> env = Collections.singletonMap("create", "true");
		final Path zipFile = temp.resolve(dirName + ".zip");
		try {
			if (Files.exists(zipFile))
				Files.delete(zipFile);
		} catch (Exception e) {
			alert.showAlert("Could not delete existing zip file: " +e , false, req);
			return null;
		}
		try {
			org.smartrplace.logging.fendodb.FendoDbFactory.class.getName();
		} catch (NoClassDefFoundError e) {
			alert.showAlert("FendoDB bundle is missing.", false, req);
			return null;
		}
        final URI uri = URI.create("jar:file:" + zipFile.toString());
		try (final FileSystem zipfs = FileSystems.newFileSystem(uri, Collections.singletonMap("create", "true"))) {
			Path folder;
			Path file;
			Path folderInZipFile;
			Path pathInZipfile;
			String filename;
			List<String> gateways = gatewaySelector.getItems(req);
			MemoryGateway gw;
			org.smartrplace.logging.fendodb.CloseableDataRecorder slots;
			for (String gwPath: gateways ) {
				try {
					gw = controller.getGateway(gwPath);
				} catch (IOException | UncheckedIOException e) {
					continue;
				}
				folder = base.resolve(gw.getId());
				Files.createDirectories(folder);
				folderInZipFile = zipfs.getPath("/" + dirName + "/" + folder.getFileName().toString());
				try {
					slots = (CloseableDataRecorder) gw.getLogdata().get();
				} catch (NoSuchElementException e) {
					continue;
				}
				Files.createDirectories(folderInZipFile);
				for (RecordedData rd: slots.getAllTimeSeries()) {
					filename = rd.getPath().replace("/", "%2F") + ".csv";
					file = folder.resolve(filename);
					toCsvFile(rd.iterator(start,end), file);
					pathInZipfile = folderInZipFile.resolve(filename);
					Files.copy(file, pathInZipfile, StandardCopyOption.REPLACE_EXISTING );
		            Files.delete(file);
				}
				FileUtils.deleteDirectory(folder.toFile());
			}
		}
		FileUtils.deleteDirectory(base.toAbsolutePath().toFile());
        return zipFile;
	}
	
	private static void toCsvFile(Iterator<SampledValue> values, Path path) throws IOException {
		try (final BufferedWriter writer = Files.newBufferedWriter(path)) {
			SampledValue sv;
			while (values.hasNext()) {
				try {
					sv = values.next();
				} catch (NoSuchElementException e) { // does in fact occur, because database may be corrupt (not ordered). We better skip those.
					LoggerFactory.getLogger(VisualizationPage.class).error("Trying to read corrupt slots data. Maybe timestamps are not ordered: " + path);
					return;
				}
				writer.write(sv.getTimestamp() + ";" + sv.getValue().getFloatValue() + "\n");
			}
			writer.flush();
		}
	}

	private final ResourceTree getResourceTree(final MemoryGateway gw) {
		try {
			return trees.get(gw, () -> new ResourceTree(gw.getAllResources().get()));
		} catch (Exception e) {
			return null;
		}
	}
	
}
