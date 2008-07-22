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
package org.springframework.batch.support;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.util.Assert;

/**
 * 
 * @author Dave Syer
 * 
 */
@SuppressWarnings("unchecked")
public class SubclassExceptionClassifier extends ExceptionClassifierSupport {
	
	private Map classified = new HashMap();

	/**
	 * Map of Throwable class types to keys for the classifier. Any subclass of
	 * the type provided will be classified as of the type given by the
	 * corresponding map entry value.
	 * 
	 * @param typeMap the typeMap to set
	 */
	public final void setTypeMap(Map typeMap) {
		Map map = new HashMap();
		for (Iterator iter = typeMap.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			addRetryableExceptionClass(entry.getKey(), entry.getValue(), map);
		}
		this.classified = map;
	}

	/**
	 * Return the value from the type map whose key is the class of the given
	 * Throwable, or its nearest ancestor if a subclass.
	 * 
	 * @see org.springframework.batch.support.ExceptionClassifierSupport#classify(java.lang.Throwable)
	 */
	public Object classify(Throwable throwable) {

		if (throwable == null) {
			return super.classify(throwable);
		}

		Class exceptionClass = throwable.getClass();
		if (classified.containsKey(exceptionClass)) {
			return classified.get(exceptionClass);
		}

		// check for subclasses
		Set classes = new TreeSet(new ClassComparator());
		classes.addAll(classified.keySet());
		for (Iterator iterator = classes.iterator(); iterator.hasNext();) {
			Class cls = (Class) iterator.next();
			if (cls.isAssignableFrom(exceptionClass)) {
				Object value = classified.get(cls);
				addRetryableExceptionClass(exceptionClass, value, this.classified);
				return value;
			}
		}

		return super.classify(throwable);
	}

	private void addRetryableExceptionClass(Object candidateClass, Object classifiedAs, Map map) {
		Assert.isAssignable(Class.class, candidateClass.getClass());
		Class exceptionClass = (Class) candidateClass;
		Assert.isAssignable(Throwable.class, exceptionClass);
		map.put(exceptionClass, classifiedAs);
	}

	/**
	 * Comparator for classes to order by inheritance.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private class ClassComparator implements Comparator {
		/**
		 * @return 1 if arg0 is assignable from arg1, -1 otherwise
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
