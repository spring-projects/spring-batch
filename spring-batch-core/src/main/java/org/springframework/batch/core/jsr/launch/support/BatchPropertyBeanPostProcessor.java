/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.jsr.launch.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.batch.api.BatchProperty;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext;
import org.springframework.batch.core.jsr.configuration.support.JsrExpressionParser;
import org.springframework.batch.core.scope.StepScope;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * <p>
 * {@link BeanPostProcessor} implementation used to inject JSR-352 String properties into batch artifact fields
 * that are marked with the {@link BatchProperty} annotation.
 * </p>
 *
 * @author Chris Schaefer
 * @author Michael Minella
 * @since 3.0
 */
@SuppressWarnings("unchecked")
public class BatchPropertyBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {
	private static final String SCOPED_TARGET_BEAN_PREFIX = "scopedTarget.";
	private static final Log LOGGER = LogFactory.getLog(BatchPropertyBeanPostProcessor.class);
	private static final Set<Class<? extends Annotation>> REQUIRED_ANNOTATIONS = new HashSet<>();

	private JsrExpressionParser jsrExpressionParser;
	private BatchPropertyContext batchPropertyContext;

	static {
		ClassLoader cl = BatchPropertyBeanPostProcessor.class.getClassLoader();

		try {
			REQUIRED_ANNOTATIONS.add((Class<? extends Annotation>) cl.loadClass("javax.inject.Inject"));
		} catch (ClassNotFoundException ex) {
			LOGGER.warn("javax.inject.Inject not found - @BatchProperty marked fields will not be processed.");
		}

		REQUIRED_ANNOTATIONS.add(BatchProperty.class);
	}

	@Override
	public Object postProcessBeforeInitialization(final Object artifact, String artifactName) throws BeansException {
		Properties artifactProperties = getArtifactProperties(artifactName);

		if (artifactProperties.isEmpty()) {
			return artifact;
		}

		injectBatchProperties(artifact, artifactProperties);

		return artifact;
	}

	@Override
	public Object postProcessAfterInitialization(Object artifact, String artifactName) throws BeansException {
		return artifact;
	}

	private Properties getArtifactProperties(String artifactName) {
		String originalArtifactName = artifactName;

		if(originalArtifactName.startsWith(SCOPED_TARGET_BEAN_PREFIX)) {
			originalArtifactName = artifactName.substring(SCOPED_TARGET_BEAN_PREFIX.length());
		}

		StepContext stepContext = StepSynchronizationManager.getContext();

		if (stepContext != null) {
			return batchPropertyContext.getStepArtifactProperties(stepContext.getStepName(), originalArtifactName);
		}

		return batchPropertyContext.getArtifactProperties(originalArtifactName);
	}

	private void injectBatchProperties(final Object artifact, final Properties artifactProperties) {
		ReflectionUtils.doWithFields(artifact.getClass(), new ReflectionUtils.FieldCallback() {
			@Override
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
				if (isValidFieldModifier(field) && isAnnotated(field)) {
					boolean isAccessible = field.isAccessible();
					field.setAccessible(true);

					String batchProperty = getBatchPropertyFieldValue(field, artifactProperties);

					if (StringUtils.hasText(batchProperty)) {
						field.set(artifact, batchProperty);
					}

					field.setAccessible(isAccessible);
				}
			}
		});
	}

	private String getBatchPropertyFieldValue(Field field, Properties batchArtifactProperties) {
		BatchProperty batchProperty = field.getAnnotation(BatchProperty.class);

		if (!"".equals(batchProperty.name())) {
			return getBatchProperty(batchProperty.name(), batchArtifactProperties);
		}

		return getBatchProperty(field.getName(), batchArtifactProperties);
	}

	private String getBatchProperty(String propertyKey, Properties batchArtifactProperties) {
		if (batchArtifactProperties.containsKey(propertyKey)) {
			String propertyValue = (String) batchArtifactProperties.get(propertyKey);

			return jsrExpressionParser.parseExpression(propertyValue);
		}

		return null;
	}

	private boolean isAnnotated(Field field) {
		for (Class<? extends Annotation> annotation : REQUIRED_ANNOTATIONS) {
			if (!field.isAnnotationPresent(annotation)) {
				return false;
			}
		}

		return true;
	}

	private boolean isValidFieldModifier(Field field) {
		return !Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers());
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
					"BatchPropertyBeanPostProcessor requires a ConfigurableListableBeanFactory");
		}

		ConfigurableListableBeanFactory configurableListableBeanFactory = (ConfigurableListableBeanFactory) beanFactory;

		BeanExpressionContext beanExpressionContext = new BeanExpressionContext(configurableListableBeanFactory,
				configurableListableBeanFactory.getBean(StepScope.class));

		this.jsrExpressionParser = new JsrExpressionParser(new StandardBeanExpressionResolver(), beanExpressionContext);
	}

	@Autowired
	public void setBatchPropertyContext(BatchPropertyContext batchPropertyContext) {
		this.batchPropertyContext = batchPropertyContext;
	}
}
