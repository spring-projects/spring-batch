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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.api.Decider;
import javax.batch.api.chunk.CheckpointAlgorithm;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.api.chunk.ItemReader;
import javax.batch.api.chunk.ItemWriter;
import javax.batch.api.chunk.listener.ChunkListener;
import javax.batch.api.chunk.listener.ItemProcessListener;
import javax.batch.api.chunk.listener.ItemReadListener;
import javax.batch.api.chunk.listener.ItemWriteListener;
import javax.batch.api.chunk.listener.RetryProcessListener;
import javax.batch.api.chunk.listener.RetryReadListener;
import javax.batch.api.chunk.listener.RetryWriteListener;
import javax.batch.api.chunk.listener.SkipProcessListener;
import javax.batch.api.chunk.listener.SkipReadListener;
import javax.batch.api.chunk.listener.SkipWriteListener;
import javax.batch.api.listener.JobListener;
import javax.batch.api.listener.StepListener;
import javax.batch.api.partition.PartitionAnalyzer;
import javax.batch.api.partition.PartitionCollector;
import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionReducer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.scope.StepScope;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.util.ReflectionUtils;

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
public class BatchPropertyBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {
	private BatchPropertyContext batchPropertyContext;
	private Log logger = LogFactory.getLog(getClass());
	private ConfigurableListableBeanFactory beanFactory;
	private BeanExpressionContext beanExpressionContext;
	private BeanExpressionResolver expressionResolver = new StandardBeanExpressionResolver();
	private Set<Class<? extends Annotation>> requiredAnnotations = new HashSet<Class<? extends Annotation>>();

	public BatchPropertyBeanPostProcessor() {
		setRequiredAnnotations();
	}

	@Override
	public Object postProcessBeforeInitialization(final Object bean, String beanName) throws BeansException {
		if (!isBatchArtifact(bean)) {
			return bean;
		}

		String beanPropertyName = getBeanPropertyName(beanName);

		final Properties artifactProperties = batchPropertyContext.getBatchProperties(beanPropertyName);

		if (artifactProperties.isEmpty()) {
			return bean;
		}

		injectBatchProperties(bean, artifactProperties);

		return bean;
	}

	private String getBeanPropertyName(String beanName) {
		StepContext stepContext = StepSynchronizationManager.getContext();

		if(stepContext != null) {
			String stepName = stepContext.getStepName();
			String jobName = stepContext.getStepExecution().getJobExecution().getJobInstance().getJobName();
			return jobName + "." + stepName + "." + beanName.substring("scopedTarget.".length());
		}

		return beanName;
	}

	@SuppressWarnings("unchecked")
	private void setRequiredAnnotations() {
		ClassLoader cl = BatchPropertyBeanPostProcessor.class.getClassLoader();

		try {
			requiredAnnotations.add((Class<? extends Annotation>) cl.loadClass("javax.inject.Inject"));
		} catch (ClassNotFoundException ex) {
			logger.warn("javax.inject.Inject not found - @BatchProperty marked fields will not be processed.");
		}

		requiredAnnotations.add(BatchProperty.class);
	}

	private boolean isBatchArtifact(Object bean) {
		return (bean instanceof ItemReader) ||
				(bean instanceof ItemProcessor) ||
				(bean instanceof ItemWriter) ||
				(bean instanceof CheckpointAlgorithm) ||
				(bean instanceof Batchlet) ||
				(bean instanceof ItemReadListener) ||
				(bean instanceof ItemProcessListener) ||
				(bean instanceof ItemWriteListener) ||
				(bean instanceof JobListener) ||
				(bean instanceof StepListener) ||
				(bean instanceof ChunkListener) ||
				(bean instanceof SkipReadListener) ||
				(bean instanceof SkipProcessListener) ||
				(bean instanceof SkipWriteListener) ||
				(bean instanceof RetryReadListener) ||
				(bean instanceof RetryProcessListener) ||
				(bean instanceof RetryWriteListener) ||
				(bean instanceof PartitionMapper) ||
				(bean instanceof PartitionReducer) ||
				(bean instanceof PartitionCollector) ||
				(bean instanceof PartitionAnalyzer) ||
				(bean instanceof PartitionPlan) ||
				(bean instanceof Decider);
	}

	private void injectBatchProperties(final Object bean, final Properties artifactProperties) {
		ReflectionUtils.doWithFields(bean.getClass(), new ReflectionUtils.FieldCallback() {
			@Override
			public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
				if (isValidFieldModifier(field) && isAnnotated(field)) {
					boolean isAccessible = field.isAccessible();
					field.setAccessible(true);

					String batchProperty = getBatchPropertyFieldValue(field, artifactProperties);

					if (batchProperty != null) {
						field.set(bean, batchProperty);
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

			return (String) expressionResolver.evaluate(propertyValue, beanExpressionContext);
		}

		return null;
	}

	private boolean isAnnotated(Field field) {
		for (Class<? extends Annotation> annotation : requiredAnnotations) {
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
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
			throw new IllegalArgumentException(
					"BatchPropertyBeanPostProcessor requires a ConfigurableListableBeanFactory");
		}

		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		this.beanExpressionContext = new BeanExpressionContext(this.beanFactory, this.beanFactory.getBean(StepScope.class));
	}

	@Autowired
	public void setBatchPropertyContext(BatchPropertyContext batchPropertyContext) {
		this.batchPropertyContext = batchPropertyContext;
	}
}
