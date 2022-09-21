/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.batch.core.configuration.annotation;

import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.configuration.support.AutomaticJobRegistrar;
import org.springframework.batch.core.configuration.support.DefaultJobLoader;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.log.LogMessage;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StopWatch;

/**
 * Base registrar that provides common infrastrucutre beans for enabling and using Spring
 * Batch in a declarative way through {@link EnableBatchProcessing}.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.0
 * @see EnableBatchProcessing
 */
class BatchRegistrar implements ImportBeanDefinitionRegistrar {

	private static final Log LOGGER = LogFactory.getLog(BatchRegistrar.class);

	private static final String MISSING_ANNOTATION_ERROR_MESSAGE = "EnableBatchProcessing is not present on importing class '%s' as expected";

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		StopWatch watch = new StopWatch();
		watch.start();
		validateState(importingClassMetadata);
		EnableBatchProcessing batchAnnotation = importingClassMetadata.getAnnotations().get(EnableBatchProcessing.class)
				.synthesize();
		String importingClassName = importingClassMetadata.getClassName();
		registerJobRepository(registry, batchAnnotation, importingClassName);
		registerJobExplorer(registry, batchAnnotation, importingClassName);
		registerJobLauncher(registry, batchAnnotation, importingClassName);
		registerJobRegistry(registry);
		registerAutomaticJobRegistrar(registry, batchAnnotation);
		watch.stop();
		LOGGER.info(LogMessage.format("Finished Spring Batch infrastrucutre beans configuration in %s ms.",
				watch.getLastTaskTimeMillis()));
	}

	private void validateState(AnnotationMetadata importingClassMetadata) {
		if (!importingClassMetadata.isAnnotated(EnableBatchProcessing.class.getName())) {
			String className = importingClassMetadata.getClassName();
			String errorMessage = String.format(MISSING_ANNOTATION_ERROR_MESSAGE, className);
			throw new IllegalStateException(errorMessage);
		}
	}

	private void registerJobRepository(BeanDefinitionRegistry registry, EnableBatchProcessing batchAnnotation,
			String importingClassName) {
		if (registry.containsBeanDefinition("jobRepository")) {
			LOGGER.info("Bean jobRepository already defined in the application context, skipping"
					+ " the registration of a jobRepository");
			return;
		}
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(JobRepositoryFactoryBean.class);

		// set mandatory properties
		String dataSourceRef = batchAnnotation.dataSourceRef();
		beanDefinitionBuilder.addPropertyReference("dataSource", dataSourceRef);

		String transactionManagerRef = batchAnnotation.transactionManagerRef();
		beanDefinitionBuilder.addPropertyReference("transactionManager", transactionManagerRef);

		// set optional properties
		String executionContextSerializerRef = batchAnnotation.executionContextSerializerRef();
		if (registry.containsBeanDefinition(executionContextSerializerRef)) {
			beanDefinitionBuilder.addPropertyReference("serializer", executionContextSerializerRef);
		}

		String lobHandlerRef = batchAnnotation.lobHandlerRef();
		if (registry.containsBeanDefinition(lobHandlerRef)) {
			beanDefinitionBuilder.addPropertyReference("lobHandler", lobHandlerRef);
		}

		String incrementerFactoryRef = batchAnnotation.incrementerFactoryRef();
		if (registry.containsBeanDefinition(incrementerFactoryRef)) {
			beanDefinitionBuilder.addPropertyReference("incrementerFactory", incrementerFactoryRef);
		}

		String charset = batchAnnotation.charset();
		if (charset != null) {
			beanDefinitionBuilder.addPropertyValue("charset", Charset.forName(charset));
		}

		String tablePrefix = batchAnnotation.tablePrefix();
		if (tablePrefix != null) {
			beanDefinitionBuilder.addPropertyValue("tablePrefix", tablePrefix);
		}

		String isolationLevelForCreate = batchAnnotation.isolationLevelForCreate();
		if (isolationLevelForCreate != null) {
			beanDefinitionBuilder.addPropertyValue("isolationLevelForCreate", isolationLevelForCreate);
		}

		beanDefinitionBuilder.addPropertyValue("maxVarCharLength", batchAnnotation.maxVarCharLength());
		beanDefinitionBuilder.addPropertyValue("clobType", batchAnnotation.clobType());
		registry.registerBeanDefinition("jobRepository", beanDefinitionBuilder.getBeanDefinition());
	}

	private void registerJobExplorer(BeanDefinitionRegistry registry, EnableBatchProcessing batchAnnotation,
			String importingClassName) {
		if (registry.containsBeanDefinition("jobExplorer")) {
			LOGGER.info("Bean jobExplorer already defined in the application context, skipping"
					+ " the registration of a jobExplorer");
			return;
		}
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(JobExplorerFactoryBean.class);

		// set mandatory properties
		String dataSourceRef = batchAnnotation.dataSourceRef();
		beanDefinitionBuilder.addPropertyReference("dataSource", dataSourceRef);

		String transactionManagerRef = batchAnnotation.transactionManagerRef();
		beanDefinitionBuilder.addPropertyReference("transactionManager", transactionManagerRef);

		// set optional properties
		String executionContextSerializerRef = batchAnnotation.executionContextSerializerRef();
		if (registry.containsBeanDefinition(executionContextSerializerRef)) {
			beanDefinitionBuilder.addPropertyReference("serializer", executionContextSerializerRef);
		}

		String lobHandlerRef = batchAnnotation.lobHandlerRef();
		if (registry.containsBeanDefinition(lobHandlerRef)) {
			beanDefinitionBuilder.addPropertyReference("lobHandler", lobHandlerRef);
		}

		String charset = batchAnnotation.charset();
		if (charset != null) {
			beanDefinitionBuilder.addPropertyValue("charset", Charset.forName(charset));
		}

		String tablePrefix = batchAnnotation.tablePrefix();
		if (tablePrefix != null) {
			beanDefinitionBuilder.addPropertyValue("tablePrefix", tablePrefix);
		}
		registry.registerBeanDefinition("jobExplorer", beanDefinitionBuilder.getBeanDefinition());
	}

	private void registerJobLauncher(BeanDefinitionRegistry registry, EnableBatchProcessing batchAnnotation,
			String importingClassName) {
		if (registry.containsBeanDefinition("jobLauncher")) {
			LOGGER.info("Bean jobLauncher already defined in the application context, skipping"
					+ " the registration of a jobLauncher");
			return;
		}
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(TaskExecutorJobLauncher.class);
		// set mandatory properties
		beanDefinitionBuilder.addPropertyReference("jobRepository", "jobRepository");

		// set optional properties
		String taskExecutorRef = batchAnnotation.taskExecutorRef();
		if (registry.containsBeanDefinition(taskExecutorRef)) {
			beanDefinitionBuilder.addPropertyReference("taskExecutor", taskExecutorRef);
		}
		registry.registerBeanDefinition("jobLauncher", beanDefinitionBuilder.getBeanDefinition());
	}

	private void registerJobRegistry(BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition("jobRegistry")) {
			LOGGER.info("Bean jobRegistry already defined in the application context, skipping"
					+ " the registration of a jobRegistry");
			return;
		}
		BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(MapJobRegistry.class)
				.getBeanDefinition();
		registry.registerBeanDefinition("jobRegistry", beanDefinition);
	}

	private void registerAutomaticJobRegistrar(BeanDefinitionRegistry registry, EnableBatchProcessing batchAnnotation) {
		if (!batchAnnotation.modular()) {
			return;
		}
		if (registry.containsBeanDefinition("jobRegistrar")) {
			LOGGER.info("Bean jobRegistrar already defined in the application context, skipping"
					+ " the registration of a jobRegistrar");
			return;
		}
		BeanDefinition jobLoaderBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(DefaultJobLoader.class)
				.addPropertyReference("jobRegistry", "jobRegistry").getBeanDefinition();
		registry.registerBeanDefinition("jobLoader", jobLoaderBeanDefinition);
		BeanDefinition jobRegistrarBeanDefinition = BeanDefinitionBuilder
				.genericBeanDefinition(AutomaticJobRegistrar.class).addPropertyReference("jobLoader", "jobLoader")
				.getBeanDefinition();
		registry.registerBeanDefinition("jobRegistrar", jobRegistrarBeanDefinition);
	}

}
