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
		wapp = widgetService.createWidgetApp("/org/smartrplace/analysis/backup/parserv2", appMan);
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
