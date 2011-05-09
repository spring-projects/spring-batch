/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.classify;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@link Classifier} for a parameterised object type based on a map.
 * Classifies objects according to their inheritance relation with the supplied
 * type map. If the object to be classified is one of the keys of the provided
 * map, or is a subclass of one of the keys, then the map entry value for that
 * key is returned. Otherwise returns the default value which is null by
 * default.
 * 
 * @author Dave Syer
 * 
 */
public class SubclassClassifier<T, C> implements Classifier<T, C> {

	private ConcurrentMap<Class<? extends T>, C> classified = new ConcurrentHashMap<Class<? extends T>, C>();

	private C defaultValue = null;

	/**
	 * Create a {@link SubclassClassifier} with null default value.
	 * 
	 */
	public SubclassClassifier() {
		this(null);
	}

	/**
	 * Create a {@link SubclassClassifier} with supplied default value.
	 * 
	 * @param defaultValue
	 */
	public SubclassClassifier(C defaultValue) {
		this(new HashMap<Class<? extends T>, C>(), defaultValue);
	}

	/**
	 * Create a {@link SubclassClassifier} with supplied default value.
	 * 
	 * @param defaultValue
	 */
	public SubclassClassifier(Map<Class<? extends T>, C> typeMap, C defaultValue) {
		super();
		this.classified = new ConcurrentHashMap<Class<? extends T>, C>(typeMap);
		this.defaultValue = defaultValue;
	}

	/**
	 * Public setter for the default value for mapping keys that are not found
	 * in the map (or their subclasses). Defaults to false.
	 * 
	 * @param defaultValue the default value to set
	 */
	public void setDefaultValue(C defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Set the classifications up as a map. The keys are types and these will be
	 * mapped along with all their subclasses to the corresponding value. The
	 * most specific types will match first.
	 * 
	 * @param map a map from type to class
	 */
	public void setTypeMap(Map<Class<? extends T>, C> map) {
		this.classified = new ConcurrentHashMap<Class<? extends T>, C>(map);
	}

	/**
	 * Return the value from the type map whose key is the class of the given
	 * Throwable, or its nearest ancestor if a subclass.
	 * 
	 */
	public C classify(T classifiable) {

		if (classifiable == null) {
			return defaultValue;
		}

		@SuppressWarnings("unchecked")
		Class<? extends T> exceptionClass = (Class<? extends T>) classifiable.getClass();
		if (classified.containsKey(exceptionClass)) {
			return classified.get(exceptionClass);
		}

		// check for subclasses
		Set<Class<? extends T>> classes = new TreeSet<Class<? extends T>>(new ClassComparator());
		classes.addAll(classified.keySet());
		for (Class<? extends T> cls : classes) {
			if (cls.isAssignableFrom(exceptionClass)) {
				C value = classified.get(cls);
				this.classified.put(exceptionClass, value);
				return value;
			}
		}

		return defaultValue;
	}

	/**
	 * Return the default value supplied in the constructor (default false).
	 */
	final public C getDefault() {
		return defaultValue;
	}

	/**
	 * Comparator for classes to order by inheritance.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private static class ClassComparator implements Comparator<Class<?>>, Serializable {
		/**
		 * @return 1 if arg0 is assignable from arg1, -1 otherwise
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Class<?> arg0, Class<?> arg1) {
			if (arg0.isAssignableFrom(arg1)) {
				return 1;
			}
			return -1;
		}
	}

}
