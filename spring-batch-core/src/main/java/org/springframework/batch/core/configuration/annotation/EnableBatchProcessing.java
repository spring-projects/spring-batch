/*
 * Copyright 2012-2025 the original author or authors.
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

import org.springframework.batch.core.configuration.support.ApplicationContextFactory;
import org.springframework.batch.core.configuration.support.AutomaticJobRegistrar;
import org.springframework.batch.core.configuration.support.GroupAwareJob;
import org.springframework.batch.core.configuration.support.ScopeConfiguration;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public Job job(JobRepository jobRepository) {
 *         return new JobBuilder(&quot;myJob&quot;, jobRepository).start(step1()).next(step2()).build();
 *     }
 *
 *     &#064;Bean
 *     protected Step step1() {
 *         ...
 *     }
 *
 *     &#064;Bean
 *     protected Step step2() {
 *         ...
 *     }
 * }
 * </pre>
 *
 * By default,this annotation configures a resouceless batch infrastructure (ie based on a
 * {@link org.springframework.batch.core.repository.support.ResourcelessJobRepository} and
 * a
 * {@link org.springframework.batch.support.transaction.ResourcelessTransactionManager}).
 *
 * Note that only one of your configuration classes needs to have the
 * <code>&#064;EnableBatchProcessing</code> annotation. Once you have an
 * <code>&#064;EnableBatchProcessing</code> class in your configuration, you have an
 * instance of {@link org.springframework.batch.core.scope.StepScope} and
 * {@link org.springframework.batch.core.scope.JobScope}, so your beans inside steps can
 * have <code>&#064;Scope("step")</code> and <code>&#064;Scope("job")</code> respectively.
 * You can also use <code>&#064;Autowired</code> to insert some useful beans into your
 * context:
 *
 * <ul>
 * <li>a {@link JobRepository} (bean name "jobRepository" of type
 * {@link org.springframework.batch.core.repository.support.ResourcelessJobRepository})</li>
 * <li>a {@link org.springframework.batch.core.launch.JobOperator} (bean name
 * "jobOperator" of type
 * {@link org.springframework.batch.core.launch.support.TaskExecutorJobOperator})</li>
 * </ul>
 *
 * Other configuration types like JDBC-based or MongoDB-based batch infrastructures can be
 * defined using store specific annotations like {@link EnableJdbcJobRepository} or
 * {@link EnableMongoJobRepository}.
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
 *     &#064;Bean
 *     public ApplicationContextFactory someJobs() {
 *         return new GenericApplicationContextFactory(SomeJobConfiguration.class);
 *     }
 *
 *     &#064;Bean
 *     public ApplicationContextFactory moreJobs() {
 *         return new GenericApplicationContextFactory(MoreJobConfiguration.class);
 *     }
 *
 *     ...
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
 *         <step id="step1" .../>
 *         <step id="step2" .../>
 *     </job>
 *     <beans:bean id="dataSource" .../>
 *     <beans:bean id="transactionManager" .../>
 *     <beans:bean id="jobOperator" class=
"org.springframework.batch.core.launch.support.JobOperatorFactoryBean">
 *         <beans:property name="jobRepository" ref="jobRepository" />
 *     </beans:bean>
 * </batch>
 * }
 * </pre>
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Taeik Lim
 * @see EnableJdbcJobRepository
 * @see EnableMongoJobRepository
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({ BatchRegistrar.class, ScopeConfiguration.class, AutomaticJobRegistrarBeanPostProcessor.class,
		BatchObservabilityBeanPostProcessor.class })
public @interface EnableBatchProcessing {

	/**
	 * Indicate whether the configuration is going to be modularized into multiple
	 * application contexts. If true, you should not create any &#64;Bean Job definitions
	 * in this context but, rather, supply them in separate (child) contexts through an
	 * {@link ApplicationContextFactory}.
	 * @return boolean indicating whether the configuration is going to be modularized
	 * into multiple application contexts. Defaults to {@code false}.
	 * @deprecated since 6.0 in favor of Spring's context hierarchies and
	 * {@link GroupAwareJob}s. Scheduled for removal in 6.2 or later.
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	boolean modular() default false;

	/**
	 * Set the task executor to use in the job operator.
	 * @return the bean name of the task executor to use. Defaults to
	 * {@literal taskExecutor}
	 */
	String taskExecutorRef() default "taskExecutor";

	/**
	 * Set the job registry to use in the job operator.
	 * @return the bean name of the job registry to use. Defaults to
	 * {@literal jobRegistry}
	 */
	String jobRegistryRef() default "jobRegistry";

	/**
	 * Set the observation registry to use in batch artifacts.
	 * @return the bean name of the observation registry to use. Defaults to
	 * {@literal observationRegistry}
	 */
	String observationRegistryRef() default "observationRegistry";

	/**
	 * Set the transaction manager to use in the job operator.
	 * @return the bean name of the transaction manager to use. Defaults to
	 * {@literal transactionManager}
	 */
	String transactionManagerRef() default "transactionManager";

	/**
	 * Set the {@link JobParametersConverter} to use in the job operator.
	 * @return the bean name of the job parameters converter to use. Defaults to
	 * {@literal jobParametersConverter}
	 * @deprecated since 6.0 with no replacement. Scheduled for removal in 6.2 or later
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	String jobParametersConverterRef() default "jobParametersConverter";

}
