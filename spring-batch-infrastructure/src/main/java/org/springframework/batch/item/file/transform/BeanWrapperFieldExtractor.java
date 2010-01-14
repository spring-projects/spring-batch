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

package org.springframework.batch.item.file.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * This is a field extractor for a java bean. Given an array of property names,
 * it will reflectively call getters on the item and return an array of all the
 * values.
 * 
 * @author Dan Garrette
 * @since 2.0
 */
public class BeanWrapperFieldExtractor<T> implements FieldExtractor<T>, InitializingBean {

	private String[] names;

	/**
	 * @param names field names to be extracted by the {@link #extract(Object)} method.
	 */
	public void setNames(String[] names) {
		Assert.notNull(names, "Names must be non-null");
		this.names = Arrays.asList(names).toArray(new String[names.length]);
	}

	/**
	 * @see org.springframework.batch.item.file.transform.FieldExtractor#extract(java.lang.Object)
	 */
	public Object[] extract(T item) {
		List<Object> values = new ArrayList<Object>();

		BeanWrapper bw = new BeanWrapperImpl(item);
		for (String propertyName : this.names) {
			values.add(bw.getPropertyValue(propertyName));
		}
		return values.toArray();
	}

	public void afterPropertiesSet() {
		Assert.notNull(names, "The 'names' property must be set.");
	}
}
