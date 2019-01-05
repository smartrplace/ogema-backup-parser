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
