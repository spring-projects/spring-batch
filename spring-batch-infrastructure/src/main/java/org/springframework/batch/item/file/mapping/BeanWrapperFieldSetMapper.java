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

package org.springframework.batch.item.file.mapping;

import java.beans.PropertyEditor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.support.DefaultPropertyEditorRegistrar;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.DataBinder;

/**
 * {@link FieldSetMapper} implementation based on bean property paths. The
 * {@link FieldSet} to be mapped should have field name meta data corresponding
 * to bean property paths in an instance of the desired type. The instance is
 * created and initialized either by referring to to a prototype object by bean
 * name in the enclosing BeanFactory, or by providing a class to instantiate
 * reflectively.<br/>
 * <br/>
 * 
 * Nested property paths, including indexed properties in maps and collections,
 * can be referenced by the {@link FieldSet} names. They will be converted to
 * nested bean properties inside the prototype. The {@link FieldSet} and the
 * prototype are thus tightly coupled by the fields that are available and those
 * that can be initialized. If some of the nested properties are optional (e.g.
 * collection members) they need to be removed by a post processor.<br/>
 * <br/>
 * 
 * To customize the way that {@link FieldSet} values are converted to the
 * desired type for injecting into the prototype there are several choices. You
 * can inject {@link PropertyEditor} instances directly through the
 * {@link #setCustomEditors(Map) customEditors} property, or you can override
 * the {@link #createBinder(Object)} and {@link #initBinder(DataBinder)}
 * methods, or you can provide a custom {@link FieldSet} implementation.<br/>
 * <br/>
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
public class BeanWrapperFieldSetMapper<T> extends DefaultPropertyEditorRegistrar implements FieldSetMapper<T>,
		BeanFactoryAware, InitializingBean {

	private String name;

	private Class<? extends T> type;

	private BeanFactory beanFactory;

	private ConcurrentMap<DistanceHolder, ConcurrentMap<String, String>> propertiesMatched = new ConcurrentHashMap<DistanceHolder, ConcurrentMap<String, String>>();

	private int distanceLimit = 5;

	private boolean strict = true;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org
	 * .springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * The maximum difference that can be tolerated in spelling between input
	 * key names and bean property names. Defaults to 5, but could be set lower
	 * if the field names match the bean names.
	 * 
	 * @param distanceLimit the distance limit to set
	 */
	public void setDistanceLimit(int distanceLimit) {
		this.distanceLimit = distanceLimit;
	}

	/**
	 * The bean name (id) for an object that can be populated from the field set
	 * that will be passed into {@link #mapFieldSet(FieldSet)}. Typically a
	 * prototype scoped bean so that a new instance is returned for each field
	 * set mapped.
	 * 
	 * Either this property or the type property must be specified, but not
	 * both.
	 * 
	 * @param name the name of a prototype bean in the enclosing BeanFactory
	 */
	public void setPrototypeBeanName(String name) {
		this.name = name;
	}

	/**
	 * Public setter for the type of bean to create instead of using a prototype
	 * bean. An object of this type will be created from its default constructor
	 * for every call to {@link #mapFieldSet(FieldSet)}.<br/>
	 * 
	 * Either this property or the prototype bean name must be specified, but
	 * not both.
	 * 
	 * @param type the type to set
	 */
	public void setTargetType(Class<? extends T> type) {
		this.type = type;
	}

	/**
	 * Check that precisely one of type or prototype bean name is specified.
	 * 
	 * @throws IllegalStateException if neither is set or both properties are
	 * set.
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.state(name != null || type != null, "Either name or type must be provided.");
		Assert.state(name == null || type == null, "Both name and type cannot be specified together.");
	}

	/**
	 * Map the {@link FieldSet} to an object retrieved from the enclosing Spring
	 * context, or to a new instance of the required type if no prototype is
	 * available.
	 * @throws BindException if there is a type conversion or other error (if
	 * the {@link DataBinder} from {@link #createBinder(Object)} has errors
	 * after binding).
	 * 
	 * @throws NotWritablePropertyException if the {@link FieldSet} contains a
	 * field that cannot be mapped to a bean property.
	 * @see org.springframework.batch.item.file.mapping.FieldSetMapper#mapFieldSet(FieldSet)
	 */
	public T mapFieldSet(FieldSet fs) throws BindException {
		T copy = getBean();
		DataBinder binder = createBinder(copy);
		binder.bind(new MutablePropertyValues(getBeanProperties(copy, fs.getProperties())));
		if (binder.getBindingResult().hasErrors()) {
			throw new BindException(binder.getBindingResult());
		}
		return copy;
	}

	/**
	 * Create a binder for the target object. The binder will then be used to
	 * bind the properties form a field set into the target object. This
	 * implementation creates a new {@link DataBinder} and calls out to
	 * {@link #initBinder(DataBinder)} and
	 * {@link #registerCustomEditors(PropertyEditorRegistry)}.
	 * 
	 * @param target
	 * @return a {@link DataBinder} that can be used to bind properties to the
	 * target.
	 */
	protected DataBinder createBinder(Object target) {
		DataBinder binder = new DataBinder(target);
		binder.setIgnoreUnknownFields(!this.strict);
		initBinder(binder);
		registerCustomEditors(binder);
		return binder;
	}

	/**
	 * Initialize a new binder instance. This hook allows customization of
	 * binder settings such as the {@link DataBinder#initDirectFieldAccess()
	 * direct field access}. Called by {@link #createBinder(Object)}.
	 * <p>
	 * Note that registration of custom property editors can be done in
	 * {@link #registerCustomEditors(PropertyEditorRegistry)}.
	 * </p>
	 * @param binder new binder instance
	 * @see #createBinder(Object)
	 */
	protected void initBinder(DataBinder binder) {
	}

	@SuppressWarnings("unchecked")
	private T getBean() {
		if (name != null) {
			return (T) beanFactory.getBean(name);
		}
		try {
			return type.newInstance();
		}
		catch (InstantiationException e) {
			ReflectionUtils.handleReflectionException(e);
		}
		catch (IllegalAccessException e) {
			ReflectionUtils.handleReflectionException(e);
		}
		// should not happen
		throw new IllegalStateException("Internal error: could not create bean instance for mapping.");
	}

	/**
	 * @param bean
	 * @param properties
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Properties getBeanProperties(Object bean, Properties properties) {

		Class<?> cls = bean.getClass();

		// Map from field names to property names
		DistanceHolder distanceKey = new DistanceHolder(cls, distanceLimit);
		if (!propertiesMatched.containsKey(distanceKey)) {
			propertiesMatched.putIfAbsent(distanceKey, new ConcurrentHashMap<String, String>());
		}
		Map<String, String> matches = new HashMap<String, String>(propertiesMatched.get(distanceKey));

		Set<String> keys = new HashSet(properties.keySet());
		for (String key : keys) {

			if (matches.containsKey(key)) {
				switchPropertyNames(properties, key, matches.get(key));
				continue;
			}

			String name = findPropertyName(bean, key);

			if (name != null) {
				if (matches.containsValue(name)) {
					throw new NotWritablePropertyException(
							cls,
							name,
							"Duplicate match with distance <= "
									+ distanceLimit
									+ " found for this property in input keys: "
									+ keys
									+ ". (Consider reducing the distance limit or changing the input key names to get a closer match.)");
				}
				matches.put(key, name);
				switchPropertyNames(properties, key, name);
			}
		}

		propertiesMatched.replace(distanceKey, new ConcurrentHashMap<String, String>(matches));
		return properties;
	}

	private String findPropertyName(Object bean, String key) {

		if (bean == null) {
			return null;
		}

		Class<?> cls = bean.getClass();

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

			Object nestedValue = getPropertyValue(bean, nestedName);
			String nestedPropertyName = findPropertyName(nestedValue, suffix);
			return nestedPropertyName == null ? null : nestedName + "." + nestedPropertyName;
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
				if (candidate.equals(prefix)) { // if it's the same don't
					// replace it...
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

	private Object getPropertyValue(Object bean, String nestedName) {
		BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);
		Object nestedValue = wrapper.getPropertyValue(nestedName);
		if (nestedValue == null) {
			try {
				nestedValue = wrapper.getPropertyType(nestedName).newInstance();
				wrapper.setPropertyValue(nestedName, nestedValue);
			}
			catch (InstantiationException e) {
				ReflectionUtils.handleReflectionException(e);
			}
			catch (IllegalAccessException e) {
				ReflectionUtils.handleReflectionException(e);
			}
		}
		return nestedValue;
	}

	private void switchPropertyNames(Properties properties, String oldName, String newName) {
		String value = properties.getProperty(oldName);
		properties.remove(oldName);
		properties.setProperty(newName, value);
	}

	/**
	 * Public setter for the 'strict' property. If true, then
	 * {@link #mapFieldSet(FieldSet)} will fail of the FieldSet contains fields
	 * that cannot be mapped to the bean.
	 * 
	 * @param strict
	 */
	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	private static class DistanceHolder {
		private final Class<?> cls;

		private final int distance;

		public DistanceHolder(Class<?> cls, int distance) {
			this.cls = cls;
			this.distance = distance;

		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((cls == null) ? 0 : cls.hashCode());
			result = prime * result + distance;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DistanceHolder other = (DistanceHolder) obj;
			if (cls == null) {
				if (other.cls != null)
					return false;
			}
			else if (!cls.equals(other.cls))
				return false;
			if (distance != other.distance)
				return false;
			return true;
		}
	}

}
