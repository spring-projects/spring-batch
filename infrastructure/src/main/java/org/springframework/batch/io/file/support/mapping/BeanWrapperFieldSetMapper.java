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

package org.springframework.batch.io.file.support.mapping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.FieldSetMapper;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * {@link FieldSetMapper} implementation based on bean property paths. The
 * {@link FieldSet} to be mapped should have field name meta data corresponding
 * to bean property paths in a prototype instance of the desired type. The
 * prototype instance is initialized by referring to to object by bean name in
 * the enclosing BeanFactory.<br/>
 * 
 * Nested property paths, including indexed properties in maps and collections,
 * can be referenced by the {@link FieldSet} names. They will be converted to
 * nested bean properties inside the prototype. The {@link FieldSet} and the
 * prototype are thus tightly coupled by the fields that are available and those
 * that can be initialized. If some of the nested properties are optional (e.g.
 * collection members) they need to be removed by a post processor.<br/>
 * 
 * Property name matching is "fuzzy" in the sense that it tolerates close
 * matches, as long as the match is unique. For instance:
 * 
 * <ul>
 * <li>Quantity = quantity (field names can be capitalised)</li>
 * <li>ISIN = isin (acronyms can be lower case bean property names, as per Java
 * Beans recommendations)</li>
 * <li>DuckPate = duckPate (capitalisation including camel casing)</li>
 * <li>ITEM_ID = itemId (capitalisation and replacing word boundary with
 * underscore)</li>
 * <li>ORDER.CUSTOMER_ID = order.customerId (nested paths are recursively
 * checked)</li>
 * </ul>
 * 
 * The algorithm used to match a property name is to start with an exact match
 * and then search successively through more distant matches until precisely one
 * match is found. If more than one match is found there will be an error.
 * 
 * @author Dave Syer
 * 
 */
public class BeanWrapperFieldSetMapper implements FieldSetMapper, BeanFactoryAware, InitializingBean {

	private String name;

	private BeanFactory beanFactory;

	private static Map propertiesMatched = new HashMap();

	private static int distanceLimit = 5;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * The bean name (id) for an object that can be populated from the field set
	 * that will be passed into {@link #mapLine(FieldSet)}. Typically a
	 * prototype scoped bean so that a new instance is returned for each field
	 * set mapped.
	 * 
	 * @param name
	 */
	public void setPrototypeBeanName(String name) {
		this.name = name;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(name);
	}

	/**
	 * Map the {@link FieldSet} to an object retrieved from the enclosing Spring
	 * context.
	 * 
	 * @throws NotWritablePropertyException if the {@link FieldSet} contains a
	 * field that cannot be mapped to a bean property.
	 * 
	 * @see org.springframework.batch.io.file.FieldSetMapper#mapLine(org.springframework.batch.io.file.FieldSet)
	 */
	public Object mapLine(FieldSet fs) {
		Object copy = beanFactory.getBean(name);
		BeanWrapper wrapper = new BeanWrapperImpl(copy);
		wrapper.setPropertyValues(getBeanProperties(copy, fs.getProperties()));
		return copy;
	}

	/**
	 * @param bean
	 * @param properties
	 * @return
	 */
	private Properties getBeanProperties(Object bean, Properties properties) {

		Class cls = bean.getClass();
		
		// Map from field names to property names
		Map matches = (Map) propertiesMatched.get(cls);
		if (matches == null) {
			matches = new HashMap();
			propertiesMatched.put(cls, matches);
		}

		Set keys = new HashSet(properties.keySet());
		for (Iterator iter = keys.iterator(); iter.hasNext();) {
			String key = (String) iter.next();

			if (matches.containsKey(key)) {
				switchPropertyNames(properties, key, (String) matches.get(key));
				continue;
			}

			String name = findPropertyName(bean, key);

			if (name != null) {
				matches.put(key, name);
				switchPropertyNames(properties, key, name);
			}
		}

		return properties;
	}

	private String findPropertyName(Object bean, String key) {

		Class cls = bean.getClass();
		
		int index = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(key);
		String prefix;
		String suffix;

		// If the property name is nested recurse down through the properties
		// looking for a match.
		if (index > 0) {
			prefix = key.substring(0, index);
			suffix = key.substring(index + 1, key.length());
			String nestedName = findPropertyName(bean, prefix);
			if (nestedName == null) {
				return null;
			}

			Object nestedValue = new BeanWrapperImpl(bean).getPropertyValue(nestedName);
			return nestedName + "." + findPropertyName(nestedValue, suffix);
		}
		
		String name = null;
		int distance = 0;
		index = key.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR);
		
		if (index > 0) {
			prefix = key.substring(0, index);
			suffix = key.substring(index);
		}
		else {
			prefix = key;
			suffix = "";
		}
		
		while (name == null && distance <= distanceLimit) {
			String[] candidates = PropertyMatches.forProperty(prefix, cls, distance).getPossibleMatches();
			// If we find precisely one match, then use that one...
			if (candidates.length == 1) {
				String candidate = candidates[0];
				if (candidate.equals(prefix)) { // if it's the same don't replace it...
					name = key;
				}
				else {
					name = candidate + suffix;
				}
			}
			distance++;
		}
		return name;
	}

	private void switchPropertyNames(Properties properties, String oldName, String newName) {
		String value = properties.getProperty(oldName);
		properties.remove(oldName);
		properties.setProperty(newName, value);
	}
}
