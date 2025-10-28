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
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.launch.support.JobOperatorFactoryBean;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.batch.core.repository.support.MongoJobRepositoryFactoryBean;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.log.LogMessage;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.transaction.annotation.Isolation;
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

	private static final String JOB_REPOSITORY = "jobRepository";

	private static final String JOB_OPERATOR = "jobOperator";

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		StopWatch watch = new StopWatch();
		watch.start();
		validateState(importingClassMetadata);
		EnableBatchProcessing batchAnnotation = importingClassMetadata.getAnnotations()
			.get(EnableBatchProcessing.class)
			.synthesize();
		registerJobRepository(registry, importingClassMetadata);
		registerJobOperator(registry, batchAnnotation);
		registerAutomaticJobRegistrar(registry, batchAnnotation);
		watch.stop();
		LOGGER.debug(LogMessage.format("Finished Spring Batch infrastructure beans configuration in %s ms.",
				watch.lastTaskInfo().getTimeMillis()));
	}

	private void validateState(AnnotationMetadata importingClassMetadata) {
		if (!importingClassMetadata.isAnnotated(EnableBatchProcessing.class.getName())) {
			String className = importingClassMetadata.getClassName();
			String errorMessage = "EnableBatchProcessing is not present on importing class '%s' as expected"
				.formatted(className);
			throw new IllegalStateException(errorMessage);
		}
	}

	private void registerJobRepository(BeanDefinitionRegistry registry, AnnotationMetadata importingClassMetadata) {
		if (registry.containsBeanDefinition(JOB_REPOSITORY)) {
			LOGGER.debug("Bean jobRepository already defined in the application context, skipping"
					+ " the registration of a jobRepository");
			return;
		}
		if (importingClassMetadata.hasAnnotation(EnableJdbcJobRepository.class.getName())) {
			registerJdbcJobRepository(registry, importingClassMetadata);
		}
		else {
			if (importingClassMetadata.hasAnnotation(EnableMongoJobRepository.class.getName())) {
				registerMongoJobRepository(registry, importingClassMetadata);
			}
			else {
				registerDefaultJobRepository(registry);
			}
		}
	}

	private void registerJdbcJobRepository(BeanDefinitionRegistry registry, AnnotationMetadata importingClassMetadata) {
		EnableJdbcJobRepository jdbcJobRepositoryAnnotation = importingClassMetadata.getAnnotations()
			.get(EnableJdbcJobRepository.class)
			.synthesize();
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
			.genericBeanDefinition(JdbcJobRepositoryFactoryBean.class);

		// set mandatory properties
		String dataSourceRef = jdbcJobRepositoryAnnotation.dataSourceRef();
		beanDefinitionBuilder.addPropertyReference("dataSource", dataSourceRef);

		String transactionManagerRef = jdbcJobRepositoryAnnotation.transactionManagerRef();
		beanDefinitionBuilder.addPropertyReference("transactionManager", transactionManagerRef);

		// set optional properties
		String executionContextSerializerRef = jdbcJobRepositoryAnnotation.executionContextSerializerRef();
		if (registry.containsBeanDefinition(executionContextSerializerRef)) {
			beanDefinitionBuilder.addPropertyReference("serializer", executionContextSerializerRef);
		}

		String conversionServiceRef = jdbcJobRepositoryAnnotation.conversionServiceRef();
		if (registry.containsBeanDefinition(conversionServiceRef)) {
			beanDefinitionBuilder.addPropertyReference("conversionService", conversionServiceRef);
		}

		String incrementerFactoryRef = jdbcJobRepositoryAnnotation.incrementerFactoryRef();
		if (registry.containsBeanDefinition(incrementerFactoryRef)) {
			beanDefinitionBuilder.addPropertyReference("incrementerFactory", incrementerFactoryRef);
		}

		String charset = jdbcJobRepositoryAnnotation.charset();
		if (charset != null) {
			beanDefinitionBuilder.addPropertyValue("charset", Charset.forName(charset));
		}

		String tablePrefix = jdbcJobRepositoryAnnotation.tablePrefix();
		if (tablePrefix != null) {
			beanDefinitionBuilder.addPropertyValue("tablePrefix", tablePrefix);
		}

		String databaseType = jdbcJobRepositoryAnnotation.databaseType();
		if (StringUtils.hasText(databaseType)) {
			beanDefinitionBuilder.addPropertyValue("databaseType", databaseType);
		}

		String jdbcOperationsRef = jdbcJobRepositoryAnnotation.jdbcOperationsRef();
		if (registry.containsBeanDefinition(jdbcOperationsRef)) {
			beanDefinitionBuilder.addPropertyReference("jdbcOperations", jdbcOperationsRef);
		}

		beanDefinitionBuilder.addPropertyValue("maxVarCharLength", jdbcJobRepositoryAnnotation.maxVarCharLength());
		beanDefinitionBuilder.addPropertyValue("clobType", jdbcJobRepositoryAnnotation.clobType());
		beanDefinitionBuilder.addPropertyValue("validateTransactionState",
				jdbcJobRepositoryAnnotation.validateTransactionState());

		Isolation isolationLevelForCreate = jdbcJobRepositoryAnnotation.isolationLevelForCreate();
		if (isolationLevelForCreate != null) {
			beanDefinitionBuilder.addPropertyValue("isolationLevelForCreateEnum", isolationLevelForCreate);
		}

		String jobKeyGeneratorRef = jdbcJobRepositoryAnnotation.jobKeyGeneratorRef();
		if (registry.containsBeanDefinition(jobKeyGeneratorRef)) {
			beanDefinitionBuilder.addPropertyReference("jobKeyGenerator", jobKeyGeneratorRef);
		}

		registry.registerBeanDefinition(JOB_REPOSITORY, beanDefinitionBuilder.getBeanDefinition());
	}

	private void registerMongoJobRepository(BeanDefinitionRegistry registry,
			AnnotationMetadata importingClassMetadata) {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
			.genericBeanDefinition(MongoJobRepositoryFactoryBean.class);
		EnableMongoJobRepository mongoJobRepositoryAnnotation = importingClassMetadata.getAnnotations()
			.get(EnableMongoJobRepository.class)
			.synthesize();
		String mongoOperationsRef = mongoJobRepositoryAnnotation.mongoOperationsRef();
		if (registry.containsBeanDefinition(mongoOperationsRef)) {
			beanDefinitionBuilder.addPropertyReference("mongoOperations", mongoOperationsRef);
		}
		String transactionManagerRef = mongoJobRepositoryAnnotation.transactionManagerRef();
		if (registry.containsBeanDefinition(transactionManagerRef)) {
			beanDefinitionBuilder.addPropertyReference("transactionManager", transactionManagerRef);
		}
		Isolation isolationLevelForCreate = mongoJobRepositoryAnnotation.isolationLevelForCreate();
		if (isolationLevelForCreate != null) {
			beanDefinitionBuilder.addPropertyValue("isolationLevelForCreate", isolationLevelForCreate);
		}

		String jobKeyGeneratorRef = mongoJobRepositoryAnnotation.jobKeyGeneratorRef();
		if (registry.containsBeanDefinition(jobKeyGeneratorRef)) {
			beanDefinitionBuilder.addPropertyReference("jobKeyGenerator", jobKeyGeneratorRef);
		}

		String jobInstanceIncrementerRef = mongoJobRepositoryAnnotation.jobInstanceIncrementerRef();
		if (registry.containsBeanDefinition(jobInstanceIncrementerRef)) {
			beanDefinitionBuilder.addPropertyReference("jobInstanceIncrementer", jobInstanceIncrementerRef);
		}

		String jobExecutionIncrementerRef = mongoJobRepositoryAnnotation.jobExecutionIncrementerRef();
		if (registry.containsBeanDefinition(jobExecutionIncrementerRef)) {
			beanDefinitionBuilder.addPropertyReference("jobExecutionIncrementer", jobExecutionIncrementerRef);
		}

		String stepExecutionIncrementerRef = mongoJobRepositoryAnnotation.stepExecutionIncrementerRef();
		if (registry.containsBeanDefinition(stepExecutionIncrementerRef)) {
			beanDefinitionBuilder.addPropertyReference("stepExecutionIncrementer", stepExecutionIncrementerRef);
		}

		beanDefinitionBuilder.addPropertyValue("validateTransactionState",
				mongoJobRepositoryAnnotation.validateTransactionState());

		registry.registerBeanDefinition(JOB_REPOSITORY, beanDefinitionBuilder.getBeanDefinition());
	}

	private void registerDefaultJobRepository(BeanDefinitionRegistry registry) {
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
			.genericBeanDefinition(ResourcelessJobRepository.class);
		registry.registerBeanDefinition(JOB_REPOSITORY, beanDefinitionBuilder.getBeanDefinition());
	}

	private void registerJobOperator(BeanDefinitionRegistry registry, EnableBatchProcessing batchAnnotation) {
		if (registry.containsBeanDefinition(JOB_OPERATOR)) {
			LOGGER.debug("Bean jobOperator already defined in the application context, skipping"
					+ " the registration of a jobOperator");
			return;
		}
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
			.genericBeanDefinition(JobOperatorFactoryBean.class);
		// set mandatory properties
		beanDefinitionBuilder.addPropertyReference(JOB_REPOSITORY, JOB_REPOSITORY);

		// set optional properties
		String jobRegistryRef = batchAnnotation.jobRegistryRef();
		if (registry.containsBeanDefinition(jobRegistryRef)) {
			beanDefinitionBuilder.addPropertyReference("jobRegistry", jobRegistryRef);
		}

		String observationRegistryRef = batchAnnotation.observationRegistryRef();
		if (registry.containsBeanDefinition(observationRegistryRef)) {
			beanDefinitionBuilder.addPropertyReference("observationRegistry", observationRegistryRef);
		}

		String transactionManagerRef = batchAnnotation.transactionManagerRef();
		if (registry.containsBeanDefinition(transactionManagerRef)) {
			beanDefinitionBuilder.addPropertyReference("transactionManager", transactionManagerRef);
		}

		String taskExecutorRef = batchAnnotation.taskExecutorRef();
		if (registry.containsBeanDefinition(taskExecutorRef)) {
			beanDefinitionBuilder.addPropertyReference("taskExecutor", taskExecutorRef);
		}
		@SuppressWarnings("removal")
		String jobParametersConverterRef = batchAnnotation.jobParametersConverterRef();
		if (registry.containsBeanDefinition(jobParametersConverterRef)) {
			beanDefinitionBuilder.addPropertyReference("jobParametersConverter", jobParametersConverterRef);
		}

		registry.registerBeanDefinition(JOB_OPERATOR, beanDefinitionBuilder.getBeanDefinition());
	}

	private void registerAutomaticJobRegistrar(BeanDefinitionRegistry registry, EnableBatchProcessing batchAnnotation) {
		if (!batchAnnotation.modular()) {
			return;
		}
		if (registry.containsBeanDefinition("jobRegistrar")) {
			LOGGER.debug("Bean jobRegistrar already defined in the application context, skipping"
					+ " the registration of a jobRegistrar");
			return;
		}
		BeanDefinition jobLoaderBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(DefaultJobLoader.class)
			.addPropertyValue("jobRegistry", new MapJobRegistry())
			.getBeanDefinition();
		registry.registerBeanDefinition("jobLoader", jobLoaderBeanDefinition);
		BeanDefinition jobRegistrarBeanDefinition = BeanDefinitionBuilder
			.genericBeanDefinition(AutomaticJobRegistrar.class)
			.addPropertyReference("jobLoader", "jobLoader")
			.getBeanDefinition();
		registry.registerBeanDefinition("jobRegistrar", jobRegistrarBeanDefinition);
	}

}
