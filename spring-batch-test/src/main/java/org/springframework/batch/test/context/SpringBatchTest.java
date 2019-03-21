/*
 * Copyright 2018 the original author or authors.
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

import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.JobScopeTestExecutionListener;
import org.springframework.batch.test.StepScopeTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Annotation that can be specified on a test class that runs Spring Batch based tests.
 * Provides the following features over the regular <em>Spring TestContext Framework</em>:
 * <ul>
 * <li>Registers a {@link JobLauncherTestUtils} bean with the
 * {@link BatchTestContextCustomizer#JOB_LAUNCHER_TEST_UTILS_BEAN_NAME} which can be used
 * in tests for launching jobs and steps.
 * </li>
 * <li>Registers a {@link JobRepositoryTestUtils} bean
 * with the {@link BatchTestContextCustomizer#JOB_REPOSITORY_TEST_UTILS_BEAN_NAME}
 * which can be used in tests setup to create or remove job executions.
 * </li>
 * <li>Registers the {@link StepScopeTestExecutionListener} and {@link JobScopeTestExecutionListener}
 * as test execution listeners which are required to test step/job scoped beans.
 * </li>
 * </ul>
 * <p>
 * A typical usage of this annotation is like:
 *
 * <pre class="code">
 * &#064;RunWith(SpringRunner.class)
 * &#064;SpringBatchTest
 * &#064;ContextConfiguration(classes = MyBatchJobConfiguration.class)
 * public class MyBatchJobTests {
 *
 *    &#064;@Autowired
 *    private JobLauncherTestUtils jobLauncherTestUtils;
 *
 *    &#064;@Autowired
 *    private JobRepositoryTestUtils jobRepositoryTestUtils;
 *
 *    &#064;Before
 *    public void clearJobExecutions() {
 *       this.jobRepositoryTestUtils.removeJobExecutions();
 *    }
 *
 *    &#064;Test
 *    public void testMyJob() throws Exception {
 *       // given
 *       JobParameters jobParameters = this.jobLauncherTestUtils.getUniqueJobParameters();
 *
 *       // when
 *       JobExecution jobExecution = this.jobLauncherTestUtils.launchJob(jobParameters);
 *
 *       // then
 *       Assert.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
 *    }
 *
 * }
 * </pre>
 *
 * @author Mahmoud Ben Hassine
 * @since 4.1
 * @see JobLauncherTestUtils
 * @see JobRepositoryTestUtils
 * @see StepScopeTestExecutionListener
 * @see JobScopeTestExecutionListener
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@TestExecutionListeners(
		listeners = {StepScopeTestExecutionListener.class, JobScopeTestExecutionListener.class},
		mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
public @interface SpringBatchTest {
}
