package org.smartrplace.backup.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

import org.ogema.serialization.jaxb.Resource;

public interface ResourceFilters {
	
	public static class NameBasedResourceFilter implements Predicate<Resource> {
		
		private final String resourceName;
		
		public NameBasedResourceFilter(String resourceName) {
			this.resourceName = resourceName;
		}

		@Override
		public boolean test(Resource resource) {
			return resourceName.equals(resource.getName());
		}
		
	}
	
	public static class TypeBasedResourceFilter implements Predicate<Resource> {
		
		private final String className;
		
		public TypeBasedResourceFilter(Class<? extends Resource> type) {
			this.className = type.getName();
		}

		@Override
		public boolean test(Resource resource) {
			return className.equals(resource.getType());
		}
		
	}
	
	/**
	 * AND concatenation of filters (all conditions must be satisfied) 
	 */
	public static class ConcatResourceFilter implements Predicate<Resource> {
		
		private final Collection<Predicate<Resource>> filters;
		
		public ConcatResourceFilter(Collection<Predicate<Resource>> filters) {
			Objects.requireNonNull(filters);
			this.filters = new ArrayList<>(filters);
		}
		
		@Override
		public boolean test(Resource resource) {
			for (Predicate<Resource> f: filters) {
				if (!f.test(resource))
					return false;
			}
			return true;
		}
		
		
	}
	
}
