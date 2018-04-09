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
package org.smartrplace.analysis.backup.parser;

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
