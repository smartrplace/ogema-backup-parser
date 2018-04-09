package org.smartrplace.ogema.memgw.visualisation;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.smartrplace.backup.parser.BackupParser;

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
