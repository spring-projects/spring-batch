/*
 * Copyright 2018-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.JobScopeTestExecutionListener;
import org.springframework.batch.test.StepScopeTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Annotation that can be specified on a test class that runs Spring Batch based tests.
 * Provides the following features over the regular <em>Spring TestContext Framework</em>:
 * <ul>
 * <li>Registers a {@link JobOperatorTestUtils} bean named "jobOperatorTestUtils" which
 * can be used in tests for starting jobs and steps.</li>
 * <li>Registers a {@link JobRepositoryTestUtils} bean named "jobRepositoryTestUtils"
 * which can be used in tests setup to create or remove job executions.</li>
 * <li>Registers the {@link StepScopeTestExecutionListener} and
 * {@link JobScopeTestExecutionListener} as test execution listeners which are required to
 * test step/job scoped beans.</li>
 * </ul>
 * <p>
 * A typical usage of this annotation with JUnit 4 is like the following:
 *
 * <pre class="code">
 * &#064;RunWith(SpringRunner.class)
 * &#064;SpringBatchTest
 * &#064;ContextConfiguration(classes = MyBatchJobConfiguration.class)
 * public class MyBatchJobTests {
 *
 *     &#064;Autowired
 *     private JobOperatorTestUtils jobOperatorTestUtils;
 *
 *     &#064;Autowired
 *     private JobRepositoryTestUtils jobRepositoryTestUtils;
 *
 *     &#064;Autowired
 *     private Job jobUnderTest;
 *
 *     &#064;Before
 *     public void setup() {
 *         this.jobRepositoryTestUtils.removeJobExecutions();
 *         this.jobOperatorTestUtils.setJob(this.jobUnderTest); // this is optional if the job is unique
 *     }
 *
 *     &#064;Test
 *     public void testMyJob() throws Exception {
 *         // given
 *         JobParameters jobParameters = this.jobOperatorTestUtils.getUniqueJobParameters();
 *
 *         // when
 *         JobExecution jobExecution = this.jobOperatorTestUtils.startJob(jobParameters);
 *
 *         // then
 *         Assert.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
 *     }
 *
 * }
 * </pre>
 *
 * For JUnit 5, this annotation can be used without manually registering the
 * {@link SpringExtension} since {@code @SpringBatchTest} is meta-annotated with
 * {@code @ExtendWith(SpringExtension.class)}. Here is an example:
 *
 * <pre class="code">
 * &#064;SpringBatchTest
 * &#064;SpringJUnitConfig(MyBatchJobConfiguration.class)
 * public class MyBatchJobTests {
 *
 *     &#064;Autowired
 *     private JobOperatorTestUtils jobOperatorTestUtils;
 *
 *     &#064;Autowired
 *     private JobRepositoryTestUtils jobRepositoryTestUtils;
 *
 *     &#064;BeforeEach
 *     public void setup(@Autowired Job jobUnderTest) {
 *         this.jobOperatorTestUtils.setJob(jobUnderTest); // this is optional if the job is unique
 *         this.jobRepositoryTestUtils.removeJobExecutions();
 *     }
 *
 *     &#064;Test
 *     public void testMyJob() throws Exception {
 *         // given
 *         JobParameters jobParameters = this.jobOperatorTestUtils.getUniqueJobParameters();
 *
 *         // when
 *         JobExecution jobExecution = this.jobOperatorTestUtils.startJob(jobParameters);
 *
 *         // then
 *         Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
 *     }
 *
 * }
 * </pre>
 *
 * It should be noted that if the test context contains a single job bean definition, that
 * is the job under test, then this annotation will set that job in the
 * {@link JobOperatorTestUtils} automatically.
 *
 * <strong>The test context must contain a <code>JobRepository</code> and a
 * <code>JobLauncher</code> beans for this annotation to properly set up test utilities.
 * In the previous example, the imported configuration class
 * <code>MyBatchJobConfiguration</code> is expected to have such beans defined in it (or
 * imported from another configuration class). </strong>
 *
 * @author Mahmoud Ben Hassine
 * @author Taeik Lim
 * @since 4.1
 * @see JobOperatorTestUtils
 * @see JobRepositoryTestUtils
 * @see StepScopeTestExecutionListener
 * @see JobScopeTestExecutionListener
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@TestExecutionListeners(listeners = { StepScopeTestExecutionListener.class, JobScopeTestExecutionListener.class },
		mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@ExtendWith(SpringExtension.class)
public @interface SpringBatchTest {

}
