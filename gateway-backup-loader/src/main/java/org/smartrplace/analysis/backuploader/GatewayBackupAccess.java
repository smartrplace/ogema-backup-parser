/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.smartrplace.analysis.backuploader;

import org.smartrplace.analysis.backuploader.resdb.TransientResourceDB;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.ogema.accesscontrol.PermissionManager;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.tools.SerializationManager;
import org.ogema.resourcemanager.impl.ApplicationResourceManager;
import org.ogema.resourcemanager.impl.ResourceDBManager;
import org.ogema.timer.TimerScheduler;
import org.ogema.tools.impl.SerializationManagerImpl;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.logging.fendodb.CloseableDataRecorder;
import org.smartrplace.logging.fendodb.FendoDbFactory;

/**
 *
 * @author jlapp
 */
@Component
public class GatewayBackupAccess implements Application {

	Logger logger = LoggerFactory.getLogger(getClass());
	
	ServiceRegistration<GatewayBackupAccess> reg;
	ComponentContext ctx;

	@Reference
	PermissionManager permissionManager;

	@Reference
	TimerScheduler timerScheduler;

	@Reference
	FendoDbFactory fendoFac;

	ApplicationManager appman;
	
	@Activate
	protected void activate(ComponentContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public void start(ApplicationManager am) {
		appman = am;
		reg = ctx.getBundleContext().registerService(GatewayBackupAccess.class, this, null);
		System.out.println("started");
	}

	void importZipBackup(SerializationManager sman, Path zipFile) throws IOException {
		FileSystem zip = FileSystems.newFileSystem(zipFile);
		List<String> ignoredFiles = Arrays.asList("OGEMA_Gateway", "RemoteSuperVisionList", "jsonOGEMAFileManagementData");
		List<Path> filesToImport = new ArrayList<>();
		for (Path root : zip.getRootDirectories()) {
			Files.find(root, 2, (path, attr) -> path.getFileName() != null
					&& path.getFileName().toString().endsWith(".ogj")
					&& !ignoredFiles.stream().anyMatch(s -> path.getFileName().toString().contains(s))).forEach(ogj -> filesToImport.add(ogj));
		}
		List<Reader> readers = new ArrayList<>();
		try {
			for (Path serializedResourceFile : filesToImport) {
				readers.add(Files.newBufferedReader(serializedResourceFile, StandardCharsets.UTF_8));
			}
			sman.createResourcesFromJson(readers);
		} finally {
			readers.forEach(r -> {
				try {
					r.close();
				} catch (IOException e) {
					logger.debug("close failed: {}", e.getMessage());
				}
			});
		}
		/*
		for (Path serializedResourceFile : filesToImport) {
			try (BufferedReader r = Files.newBufferedReader(serializedResourceFile, StandardCharsets.UTF_8)) {
				sman.createFromJson(r);
			}
		}
		 */
	}

	public GatewayBackupData getLatest(Path gwPath) throws IOException {
		Comparator<Path> filetime = (p1, p2) -> {
			try {
				return Files.getLastModifiedTime(p1).compareTo(Files.getLastModifiedTime(p2));
			} catch (IOException ioex) {
				throw new RuntimeException(ioex);
			}
		};
		Optional<Path> latestBackup = Files.find(gwPath, 1, (p, a) -> {
			String fname = p.getFileName().toString();
			return fname.startsWith("generalBackup") && fname.endsWith(".zip");
		}, FileVisitOption.FOLLOW_LINKS).sorted(filetime.reversed()).findFirst();
		System.out.println(latestBackup);
		long now = System.currentTimeMillis();
		Path slotsDbPath = gwPath.resolve("slotsdb");
		CloseableDataRecorder dataRecorder = fendoFac.getInstance(slotsDbPath, org.smartrplace.logging.fendodb.FendoDbConfigurationBuilder.getInstance()
				.setParseFoldersOnInit(true)
				.setReadOnlyMode(true)
				.build());
		logger.info("timeseries database at {} opened ({}ms)", slotsDbPath, System.currentTimeMillis() - now);
		now = System.currentTimeMillis();
		TransientResourceDB trdb = new TransientResourceDB();
		ResourceDBManager rdbman = new ResourceDBManager(trdb, dataRecorder, timerScheduler, permissionManager.getAccessManager());

		ApplicationResourceManager arm = new ApplicationResourceManager(appman, this, rdbman, permissionManager);
		SerializationManager sman = new SerializationManagerImpl(arm, arm);
		importZipBackup(sman, latestBackup.get());
		logger.info("resources imported from {} ({}ms)", latestBackup, System.currentTimeMillis() - now);
		return new GatewayBackupData(arm, dataRecorder);
	}

	@Override
	public void stop(AppStopReason asr) {
	}
	
	@Deactivate
	protected void deactivate() {
		if (reg != null) {
			reg.unregister();
		}
	}

}
