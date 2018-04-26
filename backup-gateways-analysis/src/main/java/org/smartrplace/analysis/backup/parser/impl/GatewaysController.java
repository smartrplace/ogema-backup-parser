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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;
import org.smartrplace.analysis.backup.parserv2.BackupParser;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

class GatewaysController {

	// keys: gateway ids
	private final Cache<String, MemoryGatewayImpl> gateways = CacheBuilder.newBuilder().softValues().build();
	private List<Path> paths;
	private final AtomicLong lastPathsUpdate = new AtomicLong(0);
	private final BackupParser backupParser;
	private final BundleContext ctx;
	private final Path base;
	private volatile boolean closed = false;
	
	GatewaysController(BackupParser backupParser, BundleContext ctx, Path base) {
		this.backupParser = backupParser;
		this.base = base;
		this.ctx= ctx;
	}
	
	MemoryGatewayImpl getGateway(Path path) throws IOException, UncheckedIOException {
		if (closed) 
			return null;
		String id = path.toString();
		if (id.endsWith("/") || id.endsWith("\\")) 
			id = id.substring(0, id.length()-1);
		try {
			final String id0 = id;
			return gateways.get(id0, () -> new MemoryGatewayImpl(id0, path, backupParser, ctx));
		} catch (ExecutionException e) {
			BackupAnalysis.logger.warn("Something went wrong...",e.getCause());
			return null;
		}
	}
	
	boolean containsGateway(String id) {
		return getPaths().stream().filter(path -> path.getFileName().toString().equals(id)).findAny().get() != null;
	}
	
	List<String> getIds() {
		return getPaths().stream().map(path -> path.getFileName().toString()).collect(Collectors.toList());
	}
	
	List<Path> getPaths() {
		
		return AccessController.doPrivileged(new PrivilegedAction<List<Path>>() {
			
		@Override
		public List<Path> run() {			
			
			final long now = System.currentTimeMillis();
			synchronized (lastPathsUpdate) {
				if (closed) 
					return Collections.emptyList();
				if (paths != null && lastPathsUpdate.get() - now < 60000) 
					return Collections.unmodifiableList(paths);
				try {
					List<Path> newpaths = Files.list(base).filter(path -> Files.isDirectory(path)).collect(Collectors.toList());
					lastPathsUpdate.set(now);
					GatewaysController.this.paths = newpaths;
				} catch (IOException e) {
					LoggerFactory.getLogger(BackupAnalysis.class).warn("Failed to update gateways list",e);
					if (paths == null)
						return Collections.emptyList();
				}
			}
			return Collections.unmodifiableList(paths);
		}});
	}
	
	
	void close() {
		synchronized (lastPathsUpdate) {
			closed = true;
			paths = null;
		}
		gateways.invalidateAll();
	}
	
}
