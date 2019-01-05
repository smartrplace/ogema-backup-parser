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
package org.smartrplace.analysis.backup.parserv2;

import java.util.HashMap;
import java.util.Map;

import org.ogema.model.locations.Location;
import org.ogema.model.locations.Room;
import org.ogema.serialization.jaxb.Resource;

/**
 * Not thread safe 
 * @param <R>
 */
public abstract class TreeAnalyzer<R> {
	
	// Map<Path, R>
	protected final Map<String, R> values = new HashMap<>();
	
	/**
	 * Works by side effects: add matches to values.
	 * @param resource
	 * @param current
	 * @return
	 * 		Object to be passed to child resources
	 */
	public abstract void parse(Resource resource); 

	public final Map<String, R> getMap() {
		return new HashMap<>(values);
	}
	
	/*
	 ******* Examples *****
	 */
	
	/**
	 * The {@link #getMap()} method returns a map of Rooms  
	 */
	public static class RoomAnalyzer extends TreeAnalyzer<String> {
		
		final boolean recursive;
		
		public RoomAnalyzer(boolean recursive) {
			this.recursive = recursive;
		}
		
		private final static String roomClassName = Room.class.getName();
		private final static String locationClassName = Location.class.getName();

		@Override
		public void parse(Resource resource) {
			String type = resource.getType();
			if (type.equals(roomClassName)) {
				values.put(resource.getPath(), resource.getPath());
				return;
			}
			for (Object res : resource.getSubresources()) {
				if (res instanceof Resource) { 
					Resource r = (Resource) res;
					if (r.getName().equals("location") && r.getType().equals(locationClassName)) {
						String rm = getRoomSubresource(r);
						if (rm != null) {
							values.put(resource.getPath(), rm);
						}
						break;
					}
				}
			}
		}
		
		private static String getRoomSubresource(Resource location) {
			for (Object res: location.getSubresources()) {
				if (SubresourceUtils.parseName(res).equals("room"))
					return SubresourceUtils.parsePath(res);
			}
			return null;
		}
		
		
	}
	
}
