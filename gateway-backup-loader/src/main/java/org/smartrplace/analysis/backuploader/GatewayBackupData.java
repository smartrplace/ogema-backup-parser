package org.smartrplace.analysis.backuploader;

import java.io.IOException;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.recordeddata.DataRecorder;
import org.ogema.resourcemanager.impl.ApplicationResourceManager;
import org.smartrplace.logging.fendodb.CloseableDataRecorder;

/**
 *
 * @author jlapp
 */
public class GatewayBackupData implements AutoCloseable {
	
	final ApplicationResourceManager rman;
	final CloseableDataRecorder rec;

	public GatewayBackupData(ApplicationResourceManager rman, CloseableDataRecorder rec) {
		this.rman = rman;
		this.rec = rec;
	}
	
	public ResourceAccess getResourceAccess() {
		return rman;
	}
	
	/*
	public ResourceManagement getResourceManagement() {
		return rman;
	}
	*/
	
	public DataRecorder getRecordedData() {
		return rec;
	}

	@Override
	public void close() throws IOException {
		rec.close();
	}
	
}
