/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.support;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.StringValueResolver;

/**
 * After the {@link BeanFactory} is created, this post processor will evaluate to see
 * if any of the beans referenced from a job definition (as defined by JSR-352) point
 * to class names instead of bean names.  If this is the case, a new {@link BeanDefinition}
 * is added with the name of the class as the bean name.
 *
 * This post processor will also resolve values in bean definitions that represent
 * #{jobParameter['key']} expressions prior to the standard SPeL resolution if they correspond
 * with an entry in the user provided {@link Properties} to the start or restart methods of the
 * {@link org.springframework.batch.core.jsr.launch.JsrJobOperator}. This allows jobProperty
 * replacements to occur for elements that require resolution prior to context initialization
 * and are not step scoped.
 *
 * @author Michael Minella
 * @author Chris Schaefer
 * @since 3.0
 */
public class ThreadLocalClassloaderBeanPostProcessor implements BeanFactoryPostProcessor, PriorityOrdered {
	private JobParameterResolver jobParameterResolver;
	private static final Pattern JOB_PARAMETERS_KEY_PATTERN = Pattern.compile("'([^']*?)'");
	private static final Pattern JOB_PARAMETERS_PATTERN = Pattern.compile("(#\\{jobParameters[^}]+\\})");

	public ThreadLocalClassloaderBeanPostProcessor(Properties properties) {
		this.jobParameterResolver = new JobParameterResolver(properties);
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		String[] beanNames = beanFactory.getBeanDefinitionNames();

		for (String curName : beanNames) {
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(curName);
			PropertyValue[] values = beanDefinition.getPropertyValues().getPropertyValues();

			for (PropertyValue propertyValue : values) {
				Object value = propertyValue.getValue();

				if(value instanceof RuntimeBeanReference) {
					RuntimeBeanReference ref = (RuntimeBeanReference) value;
					if(!beanFactory.containsBean(ref.getBeanName())) {
						AbstractBeanDefinition newBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(ref.getBeanName()).getBeanDefinition();
						newBeanDefinition.setScope("step");
						((DefaultListableBeanFactory) beanFactory).registerBeanDefinition(ref.getBeanName(), newBeanDefinition);
					}
				}
			}

			jobParameterResolver.visitBeanDefinition(beanDefinition);
		}
	}

	/**
	 * Sets this {@link BeanFactoryPostProcessor} to the lowest precdenece so that
	 * it is executed as late as possible in the chain of {@link BeanFactoryPostProcessor}s
	 */
	@Override
	public int getOrder() {
		return PriorityOrdered.LOWEST_PRECEDENCE;
	}

	protected class JobParameterResolver {
		private Properties properties;
		private BeanDefinitionVisitor beanDefinitionVisitor;

		public JobParameterResolver(Properties properties) {
			this.properties = properties;
			this.beanDefinitionVisitor = new BeanDefinitionVisitor(new JobParameterStringValueResolver());
		}

		public void visitBeanDefinition(BeanDefinition beanDefinition) {
			if (properties != null && ! properties.isEmpty() && ! "step".equals(beanDefinition.getScope())) {
				beanDefinitionVisitor.visitBeanDefinition(beanDefinition);
			}
		}

		protected class JobParameterStringValueResolver implements StringValueResolver {
			@Override
			public String resolveStringValue(String value) {
				if (value != null && ! "".equals(value)) {
					String resolvedString = resolveJobProperties(value);

					if (!"".equals(resolvedString)) {
						return resolvedString;
					}
				}

				return value;
			}

			private String resolveJobProperties(String value) {
				StringBuffer valueBuffer = new StringBuffer();
				Matcher jobParameterMatcher = JOB_PARAMETERS_PATTERN.matcher(value);

				while (jobParameterMatcher.find()) {
					Matcher jobParameterKeyMatcher = JOB_PARAMETERS_KEY_PATTERN.matcher(jobParameterMatcher.group(1));

					if (jobParameterKeyMatcher.find()) {
						String extractedProperty = jobParameterKeyMatcher.group(1);

						if (properties.containsKey(extractedProperty)) {
							String resolvedProperty = properties.getProperty(extractedProperty);
							jobParameterMatcher.appendReplacement(valueBuffer, resolvedProperty);
						}
					}
				}

				jobParameterMatcher.appendTail(valueBuffer);

				return valueBuffer.toString();
			}
		}
	}
}
