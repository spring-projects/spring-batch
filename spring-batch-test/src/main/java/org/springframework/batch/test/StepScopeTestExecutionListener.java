/*
 * Copyright 2006-2010 the original author or authors.
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
package org.springframework.batch.test;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * A {@link TestExecutionListener} that sets up step-scope context for
 * dependency injection into unit tests. A {@link StepContext} will be created
 * for the duration of a test method and made available to any dependencies that
 * are injected. The default behaviour is just to create a {@link JobExecution}
 * and {@link StepExecution} with fixed properties. Alternatively they can be
 * provided by the test case as a field of the correct type. If those fields are
 * not provided then an {@link ExecutionContext} for the default step execution
 * can be specified as a field of type ExecutionContext, or a field of type Map
 * (those fields can have any name but to disambiguate you can use the special
 * name "executionContext". And finally, {@link JobParameters} can be specified
 * using the same convention: a field of that type or a Map (with the field name
 * "jobParameters" used to disambiguate). Example:
 * 
 * <pre>
 * &#064;ContextConfiguration
 * &#064;TestExecutionListeners( { DependencyInjectionTestExecutionListener.class, StepScopeTestExecutionListener.class })
 * &#064;RunWith(SpringJUnit4ClassRunner.class)
 * public class StepScopeTestExecutionListenerIntegrationTests {
 * 
 * 	// A step-scoped dependency configured in the ApplicationContext
 * 	&#064;Autowired
 * 	private ItemReader&lt;String&gt; reader;
 * 
 * 	&#064;Test
 * 	public void testStepScopedReader() {
 * 		// Step context is active here so the reader can be used...
 * 		assertNotNull(reader.read());
 * 	}
 * 
 * }
 * </pre>
 * 
 * @author Dave Syer
 * 
 */
public class StepScopeTestExecutionListener implements TestExecutionListener {

	private static final String STEP_EXECUTION = StepScopeTestExecutionListener.class.getName() + ".STEP_EXECUTION";

	/**
	 * Set up a {@link StepExecution} as a test context attribute.
	 * 
	 * @param testContext the current test context
	 * @throws Exception if there is a problem
	 * @see TestExecutionListener#prepareTestInstance(TestContext)
	 */
	public void prepareTestInstance(TestContext testContext) throws Exception {
		StepExecution stepExecution = getStepExecution(testContext);
		if (stepExecution != null) {
			testContext.setAttribute(STEP_EXECUTION, stepExecution);
		}
	}

	/**
	 * @param testContext the current test context
	 * @throws Exception if there is a problem
	 * @see TestExecutionListener#beforeTestMethod(TestContext)
	 */
	public void beforeTestMethod(org.springframework.test.context.TestContext testContext) throws Exception {
		if (testContext.hasAttribute(STEP_EXECUTION)) {
			StepExecution stepExecution = (StepExecution) testContext.getAttribute(STEP_EXECUTION);
			StepSynchronizationManager.register(stepExecution);
		}

	}

	/**
	 * @param testContext the current test context
	 * @throws Exception if there is a problem
	 * @see TestExecutionListener#afterTestMethod(TestContext)
	 */
	public void afterTestMethod(TestContext testContext) throws Exception {
		if (testContext.hasAttribute(STEP_EXECUTION)) {
			StepSynchronizationManager.close();
		}
	}

	/**
	 * Discover a {@link StepExecution} as a field in the test case or create
	 * one if none is available.
	 * 
	 * @param testContext the current test context
	 * @return a {@link StepExecution}
	 */
	protected StepExecution getStepExecution(TestContext testContext) {

		Object target = testContext.getTestInstance();

		ExtractorFieldCallback extractor = new ExtractorFieldCallback(StepExecution.class, "stepExecution");
		ReflectionUtils.doWithFields(target.getClass(), extractor);
		if (extractor.getName() != null) {
			return (StepExecution) ReflectionTestUtils.getField(target, extractor.getName());
		}

		StepExecution stepExecution = null;

		extractor = new ExtractorFieldCallback(Map.class, "executionContext");
		ReflectionUtils.doWithFields(target.getClass(), extractor);

		Map<String, Object> map = null;

		if (extractor.getName() == null) {
			extractor = new ExtractorFieldCallback(ExecutionContext.class, "executionContext");
			ReflectionUtils.doWithFields(target.getClass(), extractor);
			if (extractor.getName() != null) {
				map = new HashMap<String, Object>();
				ExecutionContext executionContext = ((ExecutionContext) ReflectionTestUtils.getField(target, extractor
						.getName()));
				for (Entry<String, Object> entry : executionContext.entrySet()) {
					map.put(entry.getKey(), entry.getValue());
				}
			}
		}
		else {
			@SuppressWarnings("unchecked")
			Map<String, Object> themap = (Map<String, Object>) ReflectionTestUtils
					.getField(target, extractor.getName());
			map = themap;
		}

		JobExecution jobExecution = getJobExecution(testContext);
		if (map == null) {
			map = new HashMap<String, Object>();
		}
		if (stepExecution == null) {
			if (jobExecution != null) {
				stepExecution = jobExecution.createStepExecution("step");
			}
			else {
				stepExecution = MetaDataInstanceFactory.createStepExecution();
			}
		}
		for (String key : map.keySet()) {
			stepExecution.getExecutionContext().put(key, map.get(key));
		}

		return stepExecution;

	}

	/**
	 * Discover a {@link JobExecution} as a field in the test case or create
	 * one if none is available.
	 * 
	 * @param testContext the current test context
	 * @return a {@link JobExecution}
	 */
	private JobExecution getJobExecution(TestContext testContext) {

		Object target = testContext.getTestInstance();

		ExtractorFieldCallback extractor = new ExtractorFieldCallback(JobExecution.class, "jobExecution");
		ReflectionUtils.doWithFields(target.getClass(), extractor);
		if (extractor.getName() != null) {
			return (JobExecution) ReflectionTestUtils.getField(target, extractor.getName());
		}

		extractor = new ExtractorFieldCallback(Map.class, "jobParameters");
		ReflectionUtils.doWithFields(target.getClass(), extractor);

		Map<String, Object> map = null;

		if (extractor.getName() == null) {
			extractor = new ExtractorFieldCallback(JobParameters.class, "jobParameters");
			ReflectionUtils.doWithFields(target.getClass(), extractor);
			if (extractor.getName() != null) {
				map = new HashMap<String, Object>();
				JobParameters jobParameters = ((JobParameters) ReflectionTestUtils
						.getField(target, extractor.getName()));
				for (Entry<String, JobParameter> entry : jobParameters.getParameters().entrySet()) {
					map.put(entry.getKey(), entry.getValue().getValue());
				}
			}
		}
		else {
			@SuppressWarnings("unchecked")
			Map<String, Object> themap = (Map<String, Object>) ReflectionTestUtils
					.getField(target, extractor.getName());
			map = themap;
		}

		if (map != null) {
			Map<String, JobParameter> parameters = new HashMap<String, JobParameter>();
			for (String key : map.keySet()) {
				Object value = map.get(key);
				if (value == null) {
					parameters.put(key, new JobParameter((String) null));
				}
				else if (value instanceof String) {
					parameters.put(key, new JobParameter((String) value));
				}
				else if (value instanceof Double) {
					parameters.put(key, new JobParameter((Double) value));
				}
				else if (value instanceof Long) {
					parameters.put(key, new JobParameter((Long) value));
				}
				else if (value instanceof Date) {
					parameters.put(key, new JobParameter((Date) value));
				}
			}
			return MetaDataInstanceFactory.createJobExecution("job", 11L, 123L, new JobParameters(parameters));
		}

		return null;

	}

	/**
	 * Look for a Map in the fields provided, preferring one with the name
	 * provided.
	 */
	private final class ExtractorFieldCallback implements FieldCallback {
		private String preferredName;

		private final Class<?> preferredType;

		private Field result;

		public ExtractorFieldCallback(Class<?> preferredType, String preferredName) {
			super();
			this.preferredType = preferredType;
			this.preferredName = preferredName;
		}

		public String getName() {
			return result == null ? null : result.getName();
		}

		public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
			Class<?> type = field.getType();
			if (preferredType.isAssignableFrom(type)) {
				if (result == null || field.getName().equals(preferredName)) {
					result = field;
				}
			}
		}
	}
}
