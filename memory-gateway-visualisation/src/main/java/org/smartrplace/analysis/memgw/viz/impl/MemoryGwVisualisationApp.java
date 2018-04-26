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
package org.smartrplace.analysis.memgw.viz.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.smartrplace.analysis.backup.parserv2.BackupParser;

import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;

@Component
@Service(Application.class)
public class MemoryGwVisualisationApp implements Application {
	
	private WidgetApp wApp;
	
	@Reference
	private BackupParser backupParser;
	
	@Reference
	private OgemaGuiService widgetService;

	@Override
	public void start(ApplicationManager appManager) {
		wApp = widgetService.createWidgetApp("/org/smartrplace/memgw/visualisation", appManager);
		final WidgetPage<?> parserPage = wApp.createStartPage();
		new MemoryResourceVisualisation(parserPage, backupParser);
		
	}

	@Override
	public void stop(AppStopReason reason) {
		if (wApp != null)
			wApp.close();
		wApp = null;
	}


}
