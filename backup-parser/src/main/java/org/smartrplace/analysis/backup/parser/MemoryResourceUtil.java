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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.ogema.serialization.jaxb.BooleanArrayResource;
import org.ogema.serialization.jaxb.BooleanResource;
import org.ogema.serialization.jaxb.ByteArrayResource;
import org.ogema.serialization.jaxb.FloatArrayResource;
import org.ogema.serialization.jaxb.FloatResource;
import org.ogema.serialization.jaxb.IntegerArrayResource;
import org.ogema.serialization.jaxb.IntegerResource;
import org.ogema.serialization.jaxb.Resource;
import org.ogema.serialization.jaxb.ResourceLink;
import org.ogema.serialization.jaxb.StringArrayResource;
import org.ogema.serialization.jaxb.StringResource;
import org.ogema.serialization.jaxb.TimeArrayResource;
import org.ogema.serialization.jaxb.TimeResource;
import org.smartrplace.analysis.backup.parser.impl.ResourceLinkImpl;

public class MemoryResourceUtil {
	
	/**
	 * Find subresources
	 * @see #findResources(Collection, Predicate, boolean)
	 * @param resource
	 * @param filter
	 * @return
	 */
	public static Map<String, ResourceLink> findResourcesAsLinks(Resource resource, Predicate<Resource> filter) {
		return findResourcesAsLinks(Collections.singleton(resource), filter, true);
	}

	/**
	 * Find subresources satisfying certain conditions, within a list of resources.
	 * @param resources
	 * @param filter
	 * 		Specifies the conditions
	 * @param recursive
	 * @return
	 * 		Note: the returned ResourceLinks do not support the <code>set</code> operations, such as <code>setType</code>.
	 */
	public static Map<String, ResourceLink> findResourcesAsLinks(Collection<? extends Resource> resources, Predicate<Resource> filter, boolean recursive) {
		Objects.requireNonNull(filter);
		Map<String, ResourceLink> matches = new HashMap<>();
		for (Resource resource: resources) {
			if (filter.test(resource))
				matches.put(resource.getPath(), new ResourceLinkImpl(resource));
			if (recursive) {
				matches.putAll(findResourcesAsLinks(
						resource.getSubresources().stream()					
							.filter(object -> (object instanceof Resource)) // ResourceLink is the other possibility
							.map(object -> (Resource) object)
							.collect(Collectors.toList()),
						filter,
						recursive
					)			
				);
			}
		}
		return matches;
	}
	
	/**
	 * @see #findResources(Collection, Predicate, boolean)
	 * @param resource
	 * @param filter
	 * @return
	 */
	public static Map<String, Resource> findResourcesAs(Resource resource, Predicate<Resource> filter) {
		return findResources(Collections.singleton(resource), filter, true);
	}
	
	/**
	 * Find subresources satisfying certain conditions, within a list of resources.
	 * @param resources
	 * @param filter
	 * 		Specifies the conditions
	 * @param recursive
	 * @return
	 * 		Note: the returned ResourceLinks do not support the <code>set</code> operations, such as <code>setType</code>.
	 */
	public static Map<String, Resource> findResources(Collection<? extends Resource> resources, Predicate<Resource> filter, boolean recursive) {
		Objects.requireNonNull(filter);
		Map<String,Resource> matches = new HashMap<>();
		for (Resource resource: resources) {
			if (filter.test(resource))
				matches.put(resource.getPath(),resource);
			if (recursive) {
				matches.putAll(findResources(
						resource.getSubresources().stream()					
							.filter(object -> (object instanceof Resource)) // ResourceLink is the other possibility
							.map(object -> (Resource) object)
							.collect(Collectors.toList()),
						filter,
						recursive
					)			
				);
			}
		}
		return matches;
	}

	/**
	 * Works by side effects... after applying this method, results can be obtained from the {@link TreeAnalyzer#getMap()} method
	 * of the tree analyzers.
	 * @param resources
	 * @param anaylzers
	 */
	public static void analyzeResources(Collection<? extends Resource> resources, Collection<TreeAnalyzer<?>> anaylzers) {
		for (Resource resource: resources) {
			for (TreeAnalyzer<?> a : anaylzers) {
				a.parse(resource);
			}
			analyzeResources(resource.getSubresources().stream().filter(r -> r instanceof Resource).map(r -> (Resource) r).collect(Collectors.toList()), anaylzers);
		}
	}
	
	/**
	 * Returns a string representation of the resource value, or null, if the resource is not of value type
	 * @param object
	 * @return
	 */
	public static String getValue(Resource object) {
		if (object instanceof BooleanResource)
			return String.valueOf(((BooleanResource) object).isValue());
		if (object instanceof FloatResource)
			return String.valueOf(((FloatResource) object).getValue());
		if (object instanceof IntegerResource)
			return String.valueOf(((IntegerResource) object).getValue());
		if (object instanceof StringResource)
			return String.valueOf(((StringResource) object).getValue());
		if (object instanceof TimeResource)
			return String.valueOf(((TimeResource) object).getValue());
		if (object instanceof BooleanArrayResource)
			return String.valueOf(((BooleanArrayResource) object).getValues());
		if (object instanceof FloatArrayResource)
			return String.valueOf(((FloatArrayResource) object).getValues());
		if (object instanceof IntegerArrayResource)
			return String.valueOf(((IntegerArrayResource) object).getValues());
		if (object instanceof StringArrayResource)
			return String.valueOf(((StringArrayResource) object).getValues());
		if (object instanceof TimeArrayResource)
			return String.valueOf(((TimeArrayResource) object).getValues());
		if (object instanceof ByteArrayResource)
			return String.valueOf(((ByteArrayResource) object).getValues());
		return null;
	}
	
	
}
