/*
 * Copyright 2012-2022 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Types;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.ApplicationContextFactory;
import org.springframework.batch.core.configuration.support.AutomaticJobRegistrar;
import org.springframework.batch.core.configuration.support.ScopeConfiguration;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.context.annotation.Import;

/**
 * <p>
 * Enable Spring Batch features and provide a base configuration for setting up batch jobs
 * in an &#064;Configuration class, roughly equivalent to using the {@code <batch:*>} XML
 * namespace.
 * </p>
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableBatchProcessing
 * &#064;Import(DataSourceConfiguration.class)
 * public class AppConfig {
 *
 * 	&#064;Autowired
 * 	private JobRepository jobRepository;
 *
 * 	&#064;Bean
 * 	public Job job(JobRepository jobRepository) {
 * 		return new JobBuilder(&quot;myJob&quot;).repository(jobRepository).start(step1()).next(step2()).build();
 * 	}
 *
 * 	&#064;Bean
 *    protected Step step1() {
 *       ...
 *    }
 *
 * 	&#064;Bean
 *    protected Step step2() {
 *     ...
 *    }
 * }
 * </pre>
 *
 * This annotation configures JDBC-based Batch infrastrcuture beans, so you must provide a
 * {@link DataSource} and a
 * {@link org.springframework.transaction.PlatformTransactionManager} as a beans in the
 * application context.
 *
 * Note that only one of your configuration classes needs to have the
 * <code>&#064;EnableBatchProcessing</code> annotation. Once you have an
 * <code>&#064;EnableBatchProcessing</code> class in your configuration, you have an
 * instance of {@link StepScope} and
 * {@link org.springframework.batch.core.scope.JobScope}, so your beans inside steps can
 * have <code>&#064;Scope("step")</code> and <code>&#064;Scope("job")</code> respectively.
 * You can also use <code>&#064;Autowired</code> to insert some useful beans into your
 * context:
 *
 * <ul>
 * <li>a {@link JobRepository} (bean name "jobRepository" of type
 * {@link org.springframework.batch.core.repository.support.SimpleJobRepository})</li>
 * <li>a {@link JobLauncher} (bean name "jobLauncher" of type
 * {@link TaskExecutorJobLauncher})</li>
 * <li>a {@link JobRegistry} (bean name "jobRegistry" of type
 * {@link org.springframework.batch.core.configuration.support.MapJobRegistry})</li>
 * <li>a {@link org.springframework.batch.core.explore.JobExplorer} (bean name
 * "jobExplorer" of type
 * {@link org.springframework.batch.core.explore.support.SimpleJobExplorer})</li>
 * </ul>
 *
 * If the configuration is specified as <code>modular=true</code>, the context also
 * contains an {@link AutomaticJobRegistrar}. The job registrar is useful for modularizing
 * your configuration if there are multiple jobs. It works by creating separate child
 * application contexts to contain job configurations and register those jobs. The jobs
 * can then create steps and other dependent components without needing to worry about
 * bean definition name clashes. Beans of type {@link ApplicationContextFactory} are
 * automatically registered with the job registrar. Example:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableBatchProcessing(modular=true)
 * public class AppConfig {
 *
 *    &#064;Bean
 *    public ApplicationContextFactory someJobs() {
 *       return new GenericApplicationContextFactory(SomeJobConfiguration.class);
 *    }
 *
 *    &#064;Bean
 *    public ApplicationContextFactory moreJobs() {
 *       return new GenericApplicationContextFactory(MoreJobConfiguration.class);
 *    }
 *
 *  ...
 *
 * }
 * </pre>
 *
 * Note that a modular parent context, in general, should <em>not</em> itself contain
 * &#64;Bean definitions for job, because cyclic configuration dependencies are likely to
 * develop.
 *
 * <p>
 * For reference, compare the first example shown earlier to the following Spring XML
 * configuration:
 *
 * <pre class="code">
 * {@code
 * <batch>
 *     <job-repository />
 *     <job id="myJob">
 *       <step id="step1" .../>
 *       <step id="step2" .../>
 *     </job>
 *     <beans:bean id="dataSource" .../>
 *     <beans:bean id="transactionManager" .../>
 *     <beans:bean id="jobLauncher" class=
"org.springframework.batch.core.launch.support.TaskExecutorJobLauncher">
 *       <beans:property name="jobRepository" ref="jobRepository" />
 *     </beans:bean>
 * </batch>
 * }
 * </pre>
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({ BatchRegistrar.class, ScopeConfiguration.class, AutomaticJobRegistrarBeanPostProcessor.class })
public @interface EnableBatchProcessing {

	/**
	 * Indicate whether the configuration is going to be modularized into multiple
	 * application contexts. If true, you should not create any &#64;Bean Job definitions
	 * in this context but, rather, supply them in separate (child) contexts through an
	 * {@link ApplicationContextFactory}.
	 * @return boolean indicating whether the configuration is going to be modularized
	 * into multiple application contexts. Defaults to {@code false}.
	 */
	boolean modular() default false;

	/**
	 * Set the data source to use in the job repository and job explorer.
	 * @return the bean name of the data source to use. Default to {@literal dataSource}.
	 */
	String dataSourceRef() default "dataSource";

	/**
	 * Set the transaction manager to use in the job repository.
	 * @return the bean name of the transaction manager to use. Defaults to
	 * {@literal transactionManager}
	 */
	String transactionManagerRef() default "transactionManager";

	/**
	 * Set the execution context serializer to use in the job repository and job explorer.
	 * @return the bean name of the execution context serializer to use. Default to
	 * {@literal executionContextSerializer}.
	 */
	String executionContextSerializerRef() default "executionContextSerializer";

	/**
	 * The charset to use in the job repository and job explorer
	 * @return the charset to use. Defaults to {@literal UTF-8}.
	 */
	String charset() default "UTF-8";

	/**
	 * The Batch tables prefix. Defaults to {@literal "BATCH_"}.
	 * @return the Batch table prefix
	 */
	String tablePrefix() default AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX;

	/**
	 * The maximum lenght of exit messages in the database.
	 * @return the maximum lenght of exit messages in the database
	 */
	int maxVarCharLength() default AbstractJdbcBatchMetadataDao.DEFAULT_EXIT_MESSAGE_LENGTH;

	/**
	 * The incrementer factory to use in various DAOs.
	 * @return the bean name of the incrementer factory to use. Defaults to
	 * {@literal incrementerFactory}.
	 */
	String incrementerFactoryRef() default "incrementerFactory";

	/**
	 * The large object handler to use in job repository and job explorer.
	 * @return the bean name of the lob handler to use. Defaults to {@literal lobHandler}.
	 */
	String lobHandlerRef() default "lobHandler";

	/**
	 * The type of large objects.
	 * @return the type of large objects.
	 */
	int clobType() default Types.CLOB;

	/**
	 * Set the task executor to use in the job launcher.
	 * @return the bean name of the task executor to use. Defaults to
	 * {@literal taskExecutor}
	 */
	String taskExecutorRef() default "taskExecutor";

}
