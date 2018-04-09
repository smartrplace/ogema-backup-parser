package org.smartrplace.analysis.backup.viz.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.analysis.backup.parser.api.GatewayBackupAnalysis;
import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;

@Component
@Service(Application.class)
public class BackupVizApp implements Application {
	
	public final static Logger logger = LoggerFactory.getLogger(BackupVizApp.class);

	@Reference
	private OgemaGuiService widgetService;
	
	@Reference
	private GatewayBackupAnalysis backupAnalysis;
	
	private WidgetApp wapp;
	private Path basePath;
	
	@Override
	public void start(ApplicationManager appMan) {
		basePath = Paths.get(System.getProperty("org.smartrplace.analysis.backup.parser.basepath", "ogemaCollect/rest"));
		wapp = widgetService.createWidgetApp("/org/smartrplace/analysis/backup/parser", appMan);
		WidgetPage<?> page = wapp.createStartPage();
		new VisualizationPage(page, backupAnalysis, basePath, appMan);
	}

	@Override
	public void stop(AppStopReason arg0) {
		final WidgetApp wapp = this.wapp;
		this.wapp = null;
		this.basePath = null;
		if (wapp != null)
			wapp.close();
	}
	
}
