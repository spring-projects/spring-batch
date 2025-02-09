/*
 * Copyright 2022-2025 the original author or authors.
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
import org.springframework.batch.core.configuration.support.JobRegistrySmartInitializingSingleton;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.support.JobOperatorFactoryBean;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.log.LogMessage;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

/**
 * Base registrar that provides common infrastructure beans for enabling and using Spring
 * Batch in a declarative way through {@link EnableBatchProcessing}.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.0
 * @see EnableBatchProcessing
 */
class BatchRegistrar implements ImportBeanDefinitionRegistrar {

	private static final Log LOGGER = LogFactory.getLog(BatchRegistrar.class);

	private static final String MISSING_ANNOTATION_ERROR_MESSAGE = "EnableBatchProcessing is not present on importing class '%s' as expected";

	private static final String JOB_REPOSITORY = "jobRepository";

	private static final String JOB_EXPLORER = "jobExplorer";

	private static final String JOB_LAUNCHER = "jobLauncher";

	private static final String JOB_REGISTRY = "jobRegistry";

	private static final String JOB_LOADER = "jobLoader";

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		StopWatch watch = new StopWatch();
		watch.start();
		validateState(importingClassMetadata);
		EnableBatchProcessing batchAnnotation = importingClassMetadata.getAnnotations()
			.get(EnableBatchProcessing.class)
			.synthesize();
		registerJobRepository(registry, batchAnnotation);
		registerJobExplorer(registry, batchAnnotation);
		registerJobLauncher(registry, batchAnnotation);
		registerJobRegistry(registry);
		registerJobRegistrySmartInitializingSingleton(registry);
		registerJobOperator(registry, batchAnnotation);
		registerAutomaticJobRegistrar(registry, batchAnnotation);
		watch.stop();
		LOGGER.info(LogMessage.format("Finished Spring Batch infrastructure beans configuration in %s ms.",
				watch.lastTaskInfo().getTimeMillis()));
	}

	private void validateState(AnnotationMetadata importingClassMetadata) {
		if (!importingClassMetadata.isAnnotated(EnableBatchProcessing.class.getName())) {
			String className = importingClassMetadata.getClassName();
			String errorMessage = String.format(MISSING_ANNOTATION_ERROR_MESSAGE, className);
			throw new IllegalStateException(errorMessage);
		}
	}

	private void registerJobRepository(BeanDefinitionRegistry registry, EnableBatchProcessing batchAnnotation) {
		if (registry.containsBeanDefinition(JOB_REPOSITORY)) {
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

		String conversionServiceRef = batchAnnotation.conversionServiceRef();
		if (registry.containsBeanDefinition(conversionServiceRef)) {
			beanDefinitionBuilder.addPropertyReference("conversionService", conversionServiceRef);
		}

		String incrementerFactoryRef = batchAnnotation.incrementerFactoryRef();
		if (registry.containsBeanDefinition(incrementerFactoryRef)) {
			beanDefinitionBuilder.addPropertyReference("incrementerFactory", incrementerFactoryRef);
		}

		String jobKeyGeneratorRef = batchAnnotation.jobKeyGeneratorRef();
		if (registry.containsBeanDefinition(jobKeyGeneratorRef)) {
			beanDefinitionBuilder.addPropertyReference("jobKeyGenerator", jobKeyGeneratorRef);
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

		String databaseType = batchAnnotation.databaseType();
		if (StringUtils.hasText(databaseType)) {
			beanDefinitionBuilder.addPropertyValue("databaseType", databaseType);
		}

		beanDefinitionBuilder.addPropertyValue("maxVarCharLength", batchAnnotation.maxVarCharLength());
		beanDefinitionBuilder.addPropertyValue("clobType", batchAnnotation.clobType());
		registry.registerBeanDefinition(JOB_REPOSITORY, beanDefinitionBuilder.getBeanDefinition());
	}

	private void registerJobExplorer(BeanDefinitionRegistry registry, EnableBatchProcessing batchAnnotation) {
		if (registry.containsBeanDefinition(JOB_EXPLORER)) {
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

		String conversionServiceRef = batchAnnotation.conversionServiceRef();
		if (registry.containsBeanDefinition(conversionServiceRef)) {
			beanDefinitionBuilder.addPropertyReference("conversionService", conversionServiceRef);
		}

		String jobKeyGeneratorRef = batchAnnotation.jobKeyGeneratorRef();
		if (registry.containsBeanDefinition(jobKeyGeneratorRef)) {
			beanDefinitionBuilder.addPropertyReference("jobKeyGenerator", jobKeyGeneratorRef);
		}

		String charset = batchAnnotation.charset();
		if (charset != null) {
			beanDefinitionBuilder.addPropertyValue("charset", Charset.forName(charset));
		}

		String tablePrefix = batchAnnotation.tablePrefix();
		if (tablePrefix != null) {
			beanDefinitionBuilder.addPropertyValue("tablePrefix", tablePrefix);
		}
		registry.registerBeanDefinition(JOB_EXPLORER, beanDefinitionBuilder.getBeanDefinition());
	}

	private void registerJobLauncher(BeanDefinitionRegistry registry, EnableBatchProcessing batchAnnotation) {
		if (registry.containsBeanDefinition(JOB_LAUNCHER)) {
			LOGGER.info("Bean jobLauncher already defined in the application context, skipping"
					+ " the registration of a jobLauncher");
			return;
		}
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
			.genericBeanDefinition(TaskExecutorJobLauncher.class);
		// set mandatory properties
		beanDefinitionBuilder.addPropertyReference(JOB_REPOSITORY, JOB_REPOSITORY);

		// set optional properties
		String taskExecutorRef = batchAnnotation.taskExecutorRef();
		if (registry.containsBeanDefinition(taskExecutorRef)) {
			beanDefinitionBuilder.addPropertyReference("taskExecutor", taskExecutorRef);
		}
		registry.registerBeanDefinition(JOB_LAUNCHER, beanDefinitionBuilder.getBeanDefinition());
	}

	private void registerJobRegistry(BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(JOB_REGISTRY)) {
			LOGGER.info("Bean jobRegistry already defined in the application context, skipping"
					+ " the registration of a jobRegistry");
			return;
		}
		BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(MapJobRegistry.class)
			.getBeanDefinition();
		registry.registerBeanDefinition(JOB_REGISTRY, beanDefinition);
	}

	private void registerJobRegistrySmartInitializingSingleton(BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition("jobRegistrySmartInitializingSingleton")) {
			LOGGER
				.info("Bean jobRegistrySmartInitializingSingleton already defined in the application context, skipping"
						+ " the registration of a jobRegistrySmartInitializingSingleton");
			return;
		}
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
			.genericBeanDefinition(JobRegistrySmartInitializingSingleton.class);
		beanDefinitionBuilder.addPropertyReference(JOB_REGISTRY, JOB_REGISTRY);

		registry.registerBeanDefinition("jobRegistrySmartInitializingSingleton",
				beanDefinitionBuilder.getBeanDefinition());
	}

	private void registerJobOperator(BeanDefinitionRegistry registry, EnableBatchProcessing batchAnnotation) {
		if (registry.containsBeanDefinition("jobOperator")) {
			LOGGER.info("Bean jobOperator already defined in the application context, skipping"
					+ " the registration of a jobOperator");
			return;
		}
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
			.genericBeanDefinition(JobOperatorFactoryBean.class);
		// set mandatory properties
		String transactionManagerRef = batchAnnotation.transactionManagerRef();
		beanDefinitionBuilder.addPropertyReference("transactionManager", transactionManagerRef);

		beanDefinitionBuilder.addPropertyReference(JOB_REPOSITORY, JOB_REPOSITORY);
		beanDefinitionBuilder.addPropertyReference(JOB_LAUNCHER, JOB_LAUNCHER);
		beanDefinitionBuilder.addPropertyReference(JOB_EXPLORER, JOB_EXPLORER);
		beanDefinitionBuilder.addPropertyReference(JOB_REGISTRY, JOB_REGISTRY);

		// set optional properties
		String jobParametersConverterRef = batchAnnotation.jobParametersConverterRef();
		if (registry.containsBeanDefinition(jobParametersConverterRef)) {
			beanDefinitionBuilder.addPropertyReference("jobParametersConverter", jobParametersConverterRef);
		}

		registry.registerBeanDefinition("jobOperator", beanDefinitionBuilder.getBeanDefinition());
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
			.addPropertyReference(JOB_REGISTRY, JOB_REGISTRY)
			.getBeanDefinition();
		registry.registerBeanDefinition(JOB_LOADER, jobLoaderBeanDefinition);
		BeanDefinition jobRegistrarBeanDefinition = BeanDefinitionBuilder
			.genericBeanDefinition(AutomaticJobRegistrar.class)
			.addPropertyReference(JOB_LOADER, JOB_LOADER)
			.getBeanDefinition();
		registry.registerBeanDefinition("jobRegistrar", jobRegistrarBeanDefinition);
	}

}
