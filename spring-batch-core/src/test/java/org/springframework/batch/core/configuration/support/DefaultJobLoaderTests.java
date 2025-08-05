/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.core.configuration.support;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersIncrementer;
import org.springframework.batch.core.job.parameters.JobParametersValidator;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.StepRegistry;
import org.springframework.batch.core.step.NoSuchStepException;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Mahmoud Ben Hassine
 */
class DefaultJobLoaderTests {

	/**
	 * The name of the job as defined in the test context used in this test.
	 */
	private static final String TEST_JOB_NAME = "test-job";

	/**
	 * The name of the step as defined in the test context used in this test.
	 */
	private static final String TEST_STEP_NAME = "test-step";

	private final JobRegistry jobRegistry = new MapJobRegistry();

	private final StepRegistry stepRegistry = new MapStepRegistry();

	private final DefaultJobLoader jobLoader = new DefaultJobLoader(jobRegistry, stepRegistry);

	@Test
	void testClear() throws Exception {
		GenericApplicationContextFactory factory = new GenericApplicationContextFactory(
				new ByteArrayResource(JOB_XML.getBytes()));
		jobLoader.load(factory);
		assertEquals(1, ((Map<?, ?>) ReflectionTestUtils.getField(jobLoader, "contexts")).size());
		assertEquals(1, ((Map<?, ?>) ReflectionTestUtils.getField(jobLoader, "contextToJobNames")).size());
		jobLoader.clear();
		assertEquals(0, ((Map<?, ?>) ReflectionTestUtils.getField(jobLoader, "contexts")).size());
		assertEquals(0, ((Map<?, ?>) ReflectionTestUtils.getField(jobLoader, "contextToJobNames")).size());
	}

	@Test
	void testLoadWithExplicitName() throws Exception {
		GenericApplicationContextFactory factory = new GenericApplicationContextFactory(
				new ByteArrayResource(JOB_XML.getBytes()));
		jobLoader.load(factory);
		assertEquals(1, jobRegistry.getJobNames().size());
		jobLoader.reload(factory);
		assertEquals(1, jobRegistry.getJobNames().size());
	}

	@Test
	void createWithBothRegistries() {
		final DefaultJobLoader loader = new DefaultJobLoader();
		loader.setJobRegistry(jobRegistry);
		loader.setStepRegistry(stepRegistry);

		loader.afterPropertiesSet();
	}

	@Test
	void createWithOnlyJobRegistry() {
		final DefaultJobLoader loader = new DefaultJobLoader();
		loader.setJobRegistry(jobRegistry);

		loader.afterPropertiesSet();
	}

	@Test
	void testRegistryUpdated() throws DuplicateJobException {
		GenericApplicationContextFactory factory = new GenericApplicationContextFactory(
				new ClassPathResource("trivial-context.xml", getClass()));
		jobLoader.load(factory);
		assertEquals(1, jobRegistry.getJobNames().size());
		assertStepExist(TEST_JOB_NAME, TEST_STEP_NAME);
	}

	@Test
	void testMultipleJobsInTheSameContext() throws DuplicateJobException {
		GenericApplicationContextFactory factory = new GenericApplicationContextFactory(
				new ClassPathResource("job-context-with-steps.xml", getClass()));
		jobLoader.load(factory);
		assertEquals(2, jobRegistry.getJobNames().size());
		assertStepExist("job1", "step11", "step12");
		assertStepDoNotExist("job1", "step21", "step22");
		assertStepExist("job2", "step21", "step22");
		assertStepDoNotExist("job2", "step11", "step12");
	}

	@Test
	void testMultipleJobsInTheSameContextWithSeparateSteps() throws DuplicateJobException {
		GenericApplicationContextFactory factory = new GenericApplicationContextFactory(
				new ClassPathResource("job-context-with-separate-steps.xml", getClass()));
		jobLoader.load(factory);
		assertEquals(2, jobRegistry.getJobNames().size());
		assertStepExist("job1", "step11", "step12", "genericStep1", "genericStep2");
		assertStepDoNotExist("job1", "step21", "step22");
		assertStepExist("job2", "step21", "step22", "genericStep1", "genericStep2");
		assertStepDoNotExist("job2", "step11", "step12");
	}

	@Test
	void testNoStepRegistryAvailable() throws DuplicateJobException {
		final JobLoader loader = new DefaultJobLoader(jobRegistry);
		GenericApplicationContextFactory factory = new GenericApplicationContextFactory(
				new ClassPathResource("job-context-with-steps.xml", getClass()));
		loader.load(factory);
		// No step registry available so just registering the jobs
		assertEquals(2, jobRegistry.getJobNames().size());
	}

	@Test
	void testLoadWithJobThatIsNotAStepLocator() {
		GenericApplicationContextFactory factory = new GenericApplicationContextFactory(
				new ByteArrayResource(BASIC_JOB_XML.getBytes()));
		assertThrows(UnsupportedOperationException.class, () -> jobLoader.load(factory));
	}

	@Test
	void testLoadWithJobThatIsNotAStepLocatorNoStepRegistry() {
		final JobLoader loader = new DefaultJobLoader(jobRegistry);
		GenericApplicationContextFactory factory = new GenericApplicationContextFactory(
				new ByteArrayResource(BASIC_JOB_XML.getBytes()));
		assertDoesNotThrow(() -> loader.load(factory));
	}

	@Test
	void testReload() throws Exception {
		GenericApplicationContextFactory factory = new GenericApplicationContextFactory(
				new ClassPathResource("trivial-context.xml", getClass()));
		jobLoader.load(factory);
		assertEquals(1, jobRegistry.getJobNames().size());
		assertStepExist(TEST_JOB_NAME, TEST_STEP_NAME);
		jobLoader.reload(factory);
		assertEquals(1, jobRegistry.getJobNames().size());
		assertStepExist(TEST_JOB_NAME, TEST_STEP_NAME);
	}

	@Test
	void testReloadWithAutoRegister() throws Exception {
		GenericApplicationContextFactory factory = new GenericApplicationContextFactory(
				new ClassPathResource("trivial-context-autoregister.xml", getClass()));
		jobLoader.load(factory);
		assertEquals(1, jobRegistry.getJobNames().size());
		assertStepExist(TEST_JOB_NAME, TEST_STEP_NAME);
		jobLoader.reload(factory);
		assertEquals(1, jobRegistry.getJobNames().size());
		assertStepExist(TEST_JOB_NAME, TEST_STEP_NAME);
	}

	protected void assertStepExist(String jobName, String... stepNames) {
		for (String stepName : stepNames) {
			assertDoesNotThrow(() -> stepRegistry.getStep(jobName, stepName));
		}
	}

	protected void assertStepDoNotExist(String jobName, String... stepNames) {
		for (String stepName : stepNames) {
			assertThrows(NoSuchStepException.class, () -> stepRegistry.getStep(jobName, stepName));
		}
	}

	private static final String BASIC_JOB_XML = String.format(
			"<beans xmlns='http://www.springframework.org/schema/beans' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
					+ "xsi:schemaLocation='http://www.springframework.org/schema/beans https://www.springframework.org/schema/beans/spring-beans.xsd'><bean class='%s$BasicStubJob'/></beans>",
			DefaultJobLoaderTests.class.getName());

	private static final String JOB_XML = String.format(
			"<beans xmlns='http://www.springframework.org/schema/beans' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
					+ "xsi:schemaLocation='http://www.springframework.org/schema/beans https://www.springframework.org/schema/beans/spring-beans.xsd'><bean class='%s$StubJob'/></beans>",
			DefaultJobLoaderTests.class.getName());

	public static class BasicStubJob implements Job {

		@Override
		public void execute(JobExecution execution) {
		}

		@Nullable
		@Override
		public JobParametersIncrementer getJobParametersIncrementer() {
			return null;
		}

		@Override
		public String getName() {
			return "job";
		}

		@Override
		public boolean isRestartable() {
			return false;
		}

		@Override
		public JobParametersValidator getJobParametersValidator() {
			return null;
		}

	}

	public static class StubJob extends BasicStubJob implements StepLocator {

		@Override
		public Collection<String> getStepNames() {
			return Collections.emptyList();
		}

		@Override
		public Step getStep(String stepName) throws NoSuchStepException {
			throw new NoSuchStepException("Step [" + stepName + "] does not exist");
		}

	}

}
