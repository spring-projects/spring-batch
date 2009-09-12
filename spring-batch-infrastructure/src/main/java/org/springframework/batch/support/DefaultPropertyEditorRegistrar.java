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

import java.beans.PropertyEditor;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.config.CustomEditorConfigurer;
import org.springframework.util.ClassUtils;

/**
 * A re-usable {@link PropertyEditorRegistrar} that can be used wherever one
 * needs to register custom {@link PropertyEditor} instances with a
 * {@link PropertyEditorRegistry} (like a bean wrapper, or a type converter). It
 * is not thread safe, but useful where one is confident that binding or
 * initialisation can only be single threaded (e.g in a standalone application
 * with no threads).
 * 
 * @author Dave Syer
 * 
 */
public class DefaultPropertyEditorRegistrar implements PropertyEditorRegistrar {

	private Map<Class<?>, PropertyEditor> customEditors;

	/**
	 * Register the custom editors with the given registry.
	 * 
	 * @see org.springframework.beans.PropertyEditorRegistrar#registerCustomEditors(org.springframework.beans.PropertyEditorRegistry)
	 */
	public void registerCustomEditors(PropertyEditorRegistry registry) {
		if (this.customEditors != null) {
			for (Entry<Class<?>, PropertyEditor> entry : customEditors.entrySet()) {
				registry.registerCustomEditor(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * Specify the {@link PropertyEditor custom editors} to register.
	 * 
	 * 
	 * @param customEditors a map of Class to PropertyEditor (or class name to
	 * PropertyEditor).
	 * @see CustomEditorConfigurer#setCustomEditors(Map)
	 */
	public void setCustomEditors(Map<? extends Object, ? extends PropertyEditor> customEditors) {
		this.customEditors = new HashMap<Class<?>, PropertyEditor>();
		for (Entry<? extends Object, ? extends PropertyEditor> entry : customEditors.entrySet()) {
			Object key = entry.getKey();
			Class<?> requiredType = null;
			if (key instanceof Class<?>) {
				requiredType = (Class<?>) key;
			}
			else if (key instanceof String) {
				String className = (String) key;
				requiredType = ClassUtils.resolveClassName(className, getClass().getClassLoader());
			}
			else {
				throw new IllegalArgumentException("Invalid key [" + key
						+ "] for custom editor: needs to be Class or String.");
			}
			PropertyEditor value = entry.getValue();
			this.customEditors.put(requiredType, value);
		}
	}

}
