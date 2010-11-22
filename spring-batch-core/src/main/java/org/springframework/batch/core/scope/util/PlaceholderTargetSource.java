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
package org.springframework.batch.core.scope.util;

import java.beans.PropertyEditor;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.aop.TargetSource;
import org.springframework.aop.target.SimpleBeanTargetSource;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyEditorRegistrySupport;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.core.AttributeAccessor;
import org.springframework.core.MethodParameter;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;

/**
 * A {@link TargetSource} that lazily initializes its target, replacing bean
 * definition properties dynamically if they are marked as placeholders. String
 * values with embedded <code>%{key}</code> patterns will be replaced with the
 * corresponding value from the injected context (which must also be a String).
 * This includes dynamically locating a bean reference (e.g.
 * <code>ref="%{foo}"</code>), and partial replacement of patterns (e.g.
 * <code>value="%{foo}-bar-%{spam}"</code>). These replacements work for context
 * values that are primitive (String, Long, Integer). You can also replace
 * non-primitive values directly by making the whole bean property value into a
 * placeholder (e.g. <code>value="%{foo}"</code> where <code>foo</code> is a
 * property in the context).
 * 
 * @author Dave Syer
 * 
 */
public class PlaceholderTargetSource extends SimpleBeanTargetSource implements InitializingBean {

	/**
	 * Key for placeholders to be replaced from the properties provided.
	 */
	private static final String PLACEHOLDER_PREFIX = "%{";

	private static final String PLACEHOLDER_SUFFIX = "}";

	private ContextFactory contextFactory;

	private String beanName;

	/**
	 * Public setter for the context factory. Used to construct the context root
	 * whenever placeholders are replaced in a bean definition.
	 * 
	 * @param contextFactory the {@link ContextFactory}
	 */
	public void setContextFactory(ContextFactory contextFactory) {
		this.contextFactory = contextFactory;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() {
		Assert.notNull(contextFactory, "The ContextFactory must be set.");
		beanName = getTargetBeanName() + "#" + contextFactory.getContextId();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.aop.target.LazyInitTargetSource#getTarget()
	 */
	@Override
	public synchronized Object getTarget() throws BeansException {

		// Object target;
		Object target = getTargetFromContext();
		if (target != null) {
			return target;
		}

		DefaultListableBeanFactory listableBeanFactory = (DefaultListableBeanFactory) getBeanFactory();

		final TypeConverter typeConverter = listableBeanFactory.getTypeConverter();

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(listableBeanFactory);
		beanFactory.copyConfigurationFrom(listableBeanFactory);

		final TypeConverter contextTypeConverter = new TypeConverter() {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public Object convertIfNecessary(Object value, Class requiredType, MethodParameter methodParam)
					throws TypeMismatchException {
				Object result = null;
				if (value instanceof String) {
					String key = (String) value;
					if (key.startsWith(PLACEHOLDER_PREFIX) && key.endsWith(PLACEHOLDER_SUFFIX)) {
						key = extractKey(key);
						result = convertFromContext(key, requiredType);
						if (result == null) {
							Object property = getPropertyFromContext(key);
							// Give the normal type converter a chance by
							// reversing to a String
							if (property != null) {
								property = convertToString(property, typeConverter);
								if (property != null) {
									value = property;
								}
								logger.debug(String.format("Bound %%{%s} to String value [%s]", key, result));
							}
							else {
								throw new IllegalStateException("Cannot bind to placeholder: " + key);
							}
						}
						else {
							logger.debug(String.format("Bound %%{%s} to [%s]", key, result));
						}
					}
				}
				else if (requiredType.isAssignableFrom(value.getClass())) {
					result = value;
				}
				else if (requiredType.isAssignableFrom(String.class)) {
					result = convertToString(value, typeConverter);
					if (result == null) {
						logger.debug("Falling back on toString for conversion of : [" + value.getClass() + "]");
						result = value.toString();
					}
				}
				return result != null ? result : typeConverter.convertIfNecessary(value, requiredType, methodParam);
			}

			@SuppressWarnings("rawtypes")
			public Object convertIfNecessary(Object value, Class requiredType) throws TypeMismatchException {
				return convertIfNecessary(value, requiredType, null);
			}
		};
		beanFactory.setTypeConverter(contextTypeConverter);

		try {

			/*
			 * Need to use the merged bean definition here, otherwise it gets
			 * cached and "frozen" in and the "regular" bean definition does not
			 * come back when getBean() is called later on
			 */
			String targetBeanName = getTargetBeanName();
			GenericBeanDefinition beanDefinition = new GenericBeanDefinition(listableBeanFactory
					.getMergedBeanDefinition(targetBeanName));
			logger.debug("Rehydrating scoped target: [" + targetBeanName + "]");

			BeanDefinitionVisitor visitor = new PlaceholderBeanDefinitionVisitor(contextTypeConverter);

			beanFactory.registerBeanDefinition(beanName, beanDefinition);
			// Make the replacements before the target is hydrated
			visitor.visitBeanDefinition(beanDefinition);
			target = beanFactory.getBean(beanName);
			putTargetInContext(target);
			return target;

		}
		finally {
			beanFactory.removeBeanDefinition(beanName);
			beanFactory = null;
			// Anything else we can do to clean it up?
		}

	}

	private void putTargetInContext(Object target) {
		Object context = contextFactory.getContext();
		if (context instanceof AttributeAccessor) {
			((AttributeAccessor) context).setAttribute(beanName, target);
		}
	}

	private Object getTargetFromContext() {
		Object context = contextFactory.getContext();
		if (context instanceof AttributeAccessor) {
			return ((AttributeAccessor) context).getAttribute(beanName);
		}
		return null;
	}

	/**
	 * @param value
	 * @param typeConverter
	 * @return a String representation of the input if possible
	 */
	protected String convertToString(Object value, TypeConverter typeConverter) {
		String result = null;
		try {
			// Give it one chance to convert - this forces the default editors
			// to be registered
			result = (String) typeConverter.convertIfNecessary(value, String.class);
		}
		catch (TypeMismatchException e) {
			// ignore
		}
		if (result == null && typeConverter instanceof PropertyEditorRegistrySupport) {
			/*
			 * PropertyEditorRegistrySupport is de rigeur with TypeConverter
			 * instances used internally by Spring. If we have one of those then
			 * we can convert to String but the TypeConverter doesn't know how
			 * to.
			 */
			PropertyEditorRegistrySupport registry = (PropertyEditorRegistrySupport) typeConverter;
			PropertyEditor editor = registry.findCustomEditor(value.getClass(), null);
			if (editor != null) {
				if (registry.isSharedEditor(editor)) {
					// Synchronized access to shared editor
					// instance.
					synchronized (editor) {
						editor.setValue(value);
						result = editor.getAsText();
					}
				}
				else {
					editor.setValue(value);
					result = editor.getAsText();
				}
			}
		}
		return result;
	}

	/**
	 * @param value
	 * @param requiredType
	 * @return
	 */
	private Object convertFromContext(String key, Class<?> requiredType) {
		Object result = null;
		Object property = getPropertyFromContext(key);
		if (property == null || requiredType.isAssignableFrom(property.getClass())) {
			result = property;
		}
		return result;
	}

	private Object getPropertyFromContext(String key) {
		Object context = contextFactory.getContext();
		if (context == null) {
			throw new IllegalStateException("No context available while replacing placeholders.");
		}
		BeanWrapper wrapper = new BeanWrapperImpl(context);
		if (wrapper.isReadableProperty(key)) {
			return wrapper.getPropertyValue(key);
		}
		return null;
	}

	private String extractKey(String value) {
		return value.substring(value.indexOf(PLACEHOLDER_PREFIX) + PLACEHOLDER_PREFIX.length(), value
				.indexOf(PLACEHOLDER_SUFFIX));
	}

	/**
	 * Determine whether the input is a whole key in the form
	 * <code>%{...}</code>, i.e. starting with the correct prefix, ending with
	 * the correct suffix and containing only one of each.
	 * 
	 * @param value a String with placeholder patterns
	 * @return true if the value is a key
	 */
	private boolean isKey(String value) {
		return value.indexOf(PLACEHOLDER_PREFIX) == value.lastIndexOf(PLACEHOLDER_PREFIX)
				&& value.startsWith(PLACEHOLDER_PREFIX) && value.endsWith(PLACEHOLDER_SUFFIX);
	}

	/**
	 * A {@link BeanDefinitionVisitor} that will replace embedded placeholders
	 * with values from the provided context.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private final class PlaceholderBeanDefinitionVisitor extends BeanDefinitionVisitor {

		public PlaceholderBeanDefinitionVisitor(final TypeConverter typeConverter) {
			super(new PlaceholderStringValueResolver(typeConverter));
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		protected Object resolveValue(Object value) {

			if (value instanceof TypedStringValue) {

				TypedStringValue typedStringValue = (TypedStringValue) value;
				String stringValue = typedStringValue.getValue();
				if (stringValue != null) {

					// If the value is a whole key, try to simply replace it
					// from context.
					if (isKey(stringValue)) {
						String key = extractKey(stringValue);
						Object result = getPropertyFromContext(key);
						if (result != null) {
							value = result;
							logger.debug(String.format("Resolved %%{%s} to obtain [%s]", key, result));
						}
					}
					else {
						// Otherwise it might contain embedded keys so we try to
						// replace those
						String visitedString = resolveStringValue(stringValue);
						value = new TypedStringValue(visitedString);
					}
				}

			}
			else if (value instanceof Map) {

				Map map = (Map) value;
				Map newValue = new ManagedMap(map.size());
				newValue.putAll(map);
				super.visitMap(newValue);
				value = newValue;

			}
			else if (value instanceof List) {

				List list = (List) value;
				List newValue = new ManagedList(list.size());
				newValue.addAll(list);
				super.visitList(newValue);
				value = newValue;

			}
			else if (value instanceof Set) {

				Set list = (Set) value;
				Set newValue = new ManagedSet(list.size());
				newValue.addAll(list);
				super.visitSet(newValue);
				value = newValue;

			}
			else if (value instanceof BeanDefinition) {

				BeanDefinition newValue = new GenericBeanDefinition((BeanDefinition) value);
				visitBeanDefinition((BeanDefinition) newValue);
				value = newValue;

			}
			else if (value instanceof BeanDefinitionHolder) {

				BeanDefinition newValue = new GenericBeanDefinition(((BeanDefinitionHolder) value).getBeanDefinition());
				visitBeanDefinition((BeanDefinition) newValue);
				value = newValue;

			}
			else {

				value = super.resolveValue(value);

			}

			return value;

		}

	}

	private final class PlaceholderStringValueResolver implements StringValueResolver {

		private final TypeConverter typeConverter;

		private PlaceholderStringValueResolver(TypeConverter typeConverter) {
			this.typeConverter = typeConverter;
		}

		public String resolveStringValue(String strVal) {
			if (!strVal.contains(PLACEHOLDER_PREFIX)) {
				return strVal;
			}
			return replacePlaceholders(strVal, typeConverter);
		}

		/**
		 * Convenience method to replace all the placeholders in the input.
		 * 
		 * @param typeConverter a {@link TypeConverter} that can be used to
		 * convert placeholder keys to context values
		 * @param value the value to replace placeholders in
		 * @return the input with placeholders replaced
		 */
		private String replacePlaceholders(String value, TypeConverter typeConverter) {

			StringBuilder result = new StringBuilder(value);

			int first = result.indexOf(PLACEHOLDER_PREFIX);
			int next = result.indexOf(PLACEHOLDER_SUFFIX, first + 1);

			while (first >= 0) {

				Assert.state(next > 0, String.format("Placeholder key incorrectly specified: use %skey%s (in %s)",
						PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, value));

				String key = result.substring(first + PLACEHOLDER_PREFIX.length(), next);

				boolean replaced = replaceIfTypeMatches(result, first, next, key, String.class, typeConverter);
				replaced |= replaceIfTypeMatches(result, first, next, key, Long.class, typeConverter);
				replaced |= replaceIfTypeMatches(result, first, next, key, Integer.class, typeConverter);
				replaced |= replaceIfTypeMatches(result, first, next, key, Date.class, typeConverter);
				if (!replaced) {
					if (!value.startsWith(PLACEHOLDER_PREFIX) || !value.endsWith(PLACEHOLDER_SUFFIX)) {
						throw new IllegalStateException(String.format("Cannot bind to partial key %%{%s} in %s", key,
								value));
					}
					logger.debug(String.format("Deferring binding of placeholder: %%{%s}", key));
				}
				else {
					logger.debug(String.format("Bound %%{%s} to obtain [%s]", key, result));
				}
				first = result.indexOf(PLACEHOLDER_PREFIX, first + 1);
				next = result.indexOf(PLACEHOLDER_SUFFIX, first + 1);

			}

			return result.toString();

		}

		private boolean replaceIfTypeMatches(StringBuilder result, int first, int next, String key,
				Class<?> requiredType, TypeConverter typeConverter) {
			Object property = convertFromContext(key, requiredType);
			if (property != null) {
				result.replace(first, next + 1, (String) typeConverter.convertIfNecessary(property, String.class));
				return true;
			}
			return false;
		}

	}

}
