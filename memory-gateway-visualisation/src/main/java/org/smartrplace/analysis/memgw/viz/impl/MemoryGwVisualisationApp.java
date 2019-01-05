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
