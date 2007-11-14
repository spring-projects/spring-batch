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
package org.springframework.batch.execution.repository.dao;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.execution.runtime.DefaultJobIdentifier;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifier;
import org.springframework.util.ClassUtils;

/**
 * An {@link EntityNameLocator} that knows about {@link JobIdentifier} class
 * types and translates them into entity names that are recongnized by
 * Hibernate. The implementation is actually generic, and can be used as a
 * general purpose {@link EntityNameLocator} by setting the
 * {@link #identifierTypes} property. By default it knows about all the entity
 * types that work out of the box with Spring Batch.
 * 
 * @author Dave Syer
 * 
 */
public class JobIdentifierEntityNameLocator implements EntityNameLocator {

	private static final String SIMPLE_JOB_INSTANCE = "SimpleJobInstance";
	private Map identifierTypes;
	private Set entrySet;

	public JobIdentifierEntityNameLocator() {
		Map types = new HashMap();
		types.put(ScheduledJobIdentifier.class, "ScheduledJobInstance");
		types.put(DefaultJobIdentifier.class, "DefaultJobInstance");
		setIdentifierTypes(types);
	};

	/**
	 * Public setter for the identifier types. A map from Class (the
	 * {@link JobIdentifier} implementation) to String (the entity name). If a
	 * map from String to String is provided it will be interpreted as a map
	 * from class name to entity name.
	 * 
	 * @throws IllegalArgumentException
	 *             if a String key is provided that is not a Class name.
	 * 
	 * @param types
	 *            the identifierTypes to set
	 */
	public void setIdentifierTypes(Map types) {

		this.identifierTypes = new HashMap();
		for (Iterator iterator = types.entrySet().iterator(); iterator
				.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			Object key = entry.getKey();
			if (key instanceof Class) {
				identifierTypes.put(key, entry.getValue());
			} else {
				try {
					Class classKey = ClassUtils.forName(key.toString());
					identifierTypes.put(classKey, entry.getValue());
				} catch (ClassNotFoundException e) {
					throw new IllegalArgumentException(
							"Could not convert key in identifierTypes to type Class: ["
									+ key + "]", e);
				}
			}
		}

		this.entrySet = new TreeSet(new ClassComparator());
		entrySet.addAll(identifierTypes.keySet());

	}

	/**
	 * Identify a {@link JobInstance} entity type from the given
	 * {@link JobIdentifier} class.
	 * 
	 * @see org.springframework.batch.execution.repository.dao.EntityNameLocator#locate(java.lang.Class)
	 */
	public String locate(Class clz) {
		for (Iterator iterator = entrySet.iterator(); iterator.hasNext();) {
			Class key = (Class) iterator.next();
			if (key.isAssignableFrom(clz)) {
				return (String) identifierTypes.get(key);
			}
		}
		return SIMPLE_JOB_INSTANCE;
	}

	/**
	 * Comparator for classes to order by inheritance.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private class ClassComparator implements Comparator {
		/**
		 * @return 1 if arg0 is assignable from arg1
		 * @return -1 otherwise
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(Object arg0, Object arg1) {
			Class cls0 = (Class) arg0;
			Class cls1 = (Class) arg1;
			if (cls0.isAssignableFrom(cls1)) {
				return 1;
			}
			return -1;
		}
	}

}
