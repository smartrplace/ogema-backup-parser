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
package org.smartrplace.analysis.backup.parser.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.analysis.backup.parser.api.GatewayBackupAnalysis;
import org.smartrplace.analysis.backup.parser.api.MemoryGateway;
import org.smartrplace.analysis.backup.parser.BackupParser;

@Component
@Service(GatewayBackupAnalysis.class)
public class BackupAnalysis implements GatewayBackupAnalysis {
	
	public final static Logger logger = LoggerFactory.getLogger(BackupAnalysis.class);
	private volatile GatewaysController controller;
	
	@Reference
	private BackupParser backupParser;
	
	private volatile Path basePath;
	
	@Activate
	protected void activate(BundleContext ctx) {
		final String path0 = ctx.getProperty("org.smartrplace.analysis.backup.parser.basepath");
		final String path = path0 != null ? path0 : "ogemaCollect/rest";
		basePath = Paths.get(path);
		controller = new GatewaysController(backupParser, ctx, basePath);
	}
	
	@Deactivate
	protected void deactivate() {
		final GatewaysController controller = this.controller;
		this.controller = null;
		this.basePath = null;
		if (controller != null)
			controller.close();
	}

	@Override
	public List<String> getGatewayIds() {
		return controller.getIds();
	}

	@Override
	public boolean hasGateway(String id) {
		return controller.containsGateway(id);
	}

	@Override
	public MemoryGateway getGateway(String id) throws IOException, UncheckedIOException  {
		return controller.getGateway(basePath.resolve(id));
	}
	
}
