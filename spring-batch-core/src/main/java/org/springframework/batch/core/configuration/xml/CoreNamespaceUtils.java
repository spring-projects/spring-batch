/*
 * Copyright 2006-2009 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import java.util.Map;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Utility methods used in parsing of the batch core namespace
 * 
 * @author Thomas Risberg
 */
public class CoreNamespaceUtils {

	private static final String STEP_SCOPE_PROCESSOR_BEAN_NAME = "org.springframework.batch.core.scope.internalStepScope";

	private static final String STEP_SCOPE_PROCESSOR_CLASS_NAME = "org.springframework.batch.core.scope.StepScope";

	private static final String CUSTOM_EDITOR_CONFIGURER_CLASS_NAME = "org.springframework.beans.factory.config.CustomEditorConfigurer";

	private static final String RANGE_ARRAY_CLASS_NAME = "org.springframework.batch.item.file.transform.Range[]";

	private static final String RANGE_ARRAY_EDITOR_CLASS_NAME = "org.springframework.batch.item.file.transform.RangeArrayPropertyEditor";

	private static final String CORE_NAMESPACE_POST_PROCESSOR_CLASS_NAME = "org.springframework.batch.core.configuration.xml.CoreNamespacePostProcessor";

	protected static void autoregisterBeansForNamespace(ParserContext parserContext, Object source) {
		checkForStepScope(parserContext, source);
		addRangePropertyEditor(parserContext);
		addCoreNamespacePostProcessor(parserContext);
	}

	private static void checkForStepScope(ParserContext parserContext, Object source) {
		boolean foundStepScope = false;
		String[] beanNames = parserContext.getRegistry().getBeanDefinitionNames();
		for (String beanName : beanNames) {
			BeanDefinition bd = parserContext.getRegistry().getBeanDefinition(beanName);
			if (STEP_SCOPE_PROCESSOR_CLASS_NAME.equals(bd.getBeanClassName())) {
				foundStepScope = true;
				break;
			}
		}
		if (!foundStepScope) {
			BeanDefinitionBuilder stepScopeBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(STEP_SCOPE_PROCESSOR_CLASS_NAME);
			AbstractBeanDefinition abd = stepScopeBuilder.getBeanDefinition();
			abd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			abd.setSource(source);
			parserContext.getRegistry().registerBeanDefinition(STEP_SCOPE_PROCESSOR_BEAN_NAME, abd);
		}
	}

	/**
	 * Register a RangePropertyEditor if one does not already exist.
	 * 
	 * @param parserContext
	 */
	@SuppressWarnings("unchecked")
	private static void addRangePropertyEditor(ParserContext parserContext) {
		BeanDefinitionRegistry registry = parserContext.getRegistry();
		if (!rangeArrayEditorAlreadyDefined(registry)) {
			AbstractBeanDefinition customEditorConfigurer = BeanDefinitionBuilder.genericBeanDefinition(
					CUSTOM_EDITOR_CONFIGURER_CLASS_NAME).getBeanDefinition();
			customEditorConfigurer.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			ManagedMap editors = new ManagedMap();
			editors.put(RANGE_ARRAY_CLASS_NAME, RANGE_ARRAY_EDITOR_CLASS_NAME);
			customEditorConfigurer.getPropertyValues().addPropertyValue("customEditors", editors);
			registry.registerBeanDefinition(CUSTOM_EDITOR_CONFIGURER_CLASS_NAME, customEditorConfigurer);
		}
	}

	private static boolean rangeArrayEditorAlreadyDefined(BeanDefinitionRegistry registry) {
		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition bd = registry.getBeanDefinition(beanName);
			if (CUSTOM_EDITOR_CONFIGURER_CLASS_NAME.equals(bd.getBeanClassName())) {
				PropertyValue pv = bd.getPropertyValues().getPropertyValue("customEditors");
				if (pv != null) {
					for (Map.Entry<?, ?> entry : ((Map<?, ?>) pv.getValue()).entrySet()) {
						if (entry.getKey() instanceof TypedStringValue) {
							if (RANGE_ARRAY_CLASS_NAME.equals(((TypedStringValue) entry.getKey()).getValue())) {
								return true;
							}
						}
						else if (entry.getKey() instanceof String) {
							if (RANGE_ARRAY_CLASS_NAME.equals(entry.getKey())) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * @param parserContext
	 */
	private static void addCoreNamespacePostProcessor(ParserContext parserContext) {
		BeanDefinitionRegistry registry = parserContext.getRegistry();
		if (!coreNamespaceBeanPostProcessorAlreadyDefined(registry)) {
			AbstractBeanDefinition postProcessorBeanDef = BeanDefinitionBuilder.genericBeanDefinition(
					CORE_NAMESPACE_POST_PROCESSOR_CLASS_NAME).getBeanDefinition();
			postProcessorBeanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(CORE_NAMESPACE_POST_PROCESSOR_CLASS_NAME, postProcessorBeanDef);
		}
	}

	private static boolean coreNamespaceBeanPostProcessorAlreadyDefined(BeanDefinitionRegistry registry) {
		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition bd = registry.getBeanDefinition(beanName);
			if (CORE_NAMESPACE_POST_PROCESSOR_CLASS_NAME.equals(bd.getBeanClassName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Should this element be treated as incomplete? If it has a parent or is
	 * abstract, then it may not have all properties.
	 * 
	 * @param element
	 * @return TRUE if the element is abstract or has a parent
	 */
	public static boolean isUnderspecified(Element element) {
		return isAbstract(element) || StringUtils.hasText(element.getAttribute("parent"));
	}

	/**
	 * @param element
	 * @return TRUE if the element is abstract
	 */
	public static boolean isAbstract(Element element) {
		String abstractAttr = element.getAttribute("abstract");
		return StringUtils.hasText(abstractAttr) && Boolean.valueOf(abstractAttr);
	}

	/**
	 * Check that the schema location declared in the source file being parsed
	 * matches the Spring Batch version. (The old 2.0 schema is not 100%
	 * compatible with the new parser, so it is an error to explicitly define
	 * 2.0. It might be an error to declare spring-batch.xsd as an alias, but
	 * you are only going to find that out when one of the sub parses breaks.)
	 * 
	 * @param element the element that is to be parsed next
	 * @return true if we find a schema declaration that matches
	 */
	public static boolean namespaceMatchesVersion(Element element) {
		return matchesVersionInternal(element)
				&& matchesVersionInternal(element.getOwnerDocument().getDocumentElement());
	}

	private static boolean matchesVersionInternal(Element element) {
		String schemaLocation = element.getAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation");
		return schemaLocation.matches("(?m).*spring-batch-2.1.xsd.*")
				|| schemaLocation.matches("(?m).*spring-batch.xsd.*")
				|| !schemaLocation.matches("(?m).*spring-batch.*");
	}

}
