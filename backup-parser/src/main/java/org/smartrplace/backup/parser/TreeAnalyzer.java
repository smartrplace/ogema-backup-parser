package org.smartrplace.backup.parser;

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
