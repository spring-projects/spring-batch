/*
 * Copyright 2006-2014 the original author or authors.
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
package org.springframework.batch.test;

import java.lang.reflect.Method;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.adapter.HippyMethodInvoker;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * A {@link TestExecutionListener} that sets up step-scope context for
 * dependency injection into unit tests. A {@link StepContext} will be created
 * for the duration of a test method and made available to any dependencies that
 * are injected. The default behaviour is just to create a {@link StepExecution}
 * with fixed properties. Alternatively it can be provided by the test case as a
 * factory methods returning the correct type.  Example:
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
 *  public StepExecution getStepExecution() {
 *    StepExecution execution = MetaDataInstanceFactory.createStepExecution();
 *    execution.getExecutionContext().putString("foo", "bar");
 *    return execution;
 *  }
 * 
 * 	&#064;Test
 * 	public void testStepScopedReader() {
 * 		// Step context is active here so the reader can be used,
 *      // and the step execution context will contain foo=bar...
 * 		assertNotNull(reader.read());
 * 	}
 * 
 * }
 * </pre>
 * 
 * @author Dave Syer
 * @author Chris Schaefer
 */
public class StepScopeTestExecutionListener implements TestExecutionListener {
	private static final String STEP_EXECUTION = StepScopeTestExecutionListener.class.getName() + ".STEP_EXECUTION";
	private static final String SET_ATTRIBUTE_METHOD_NAME = "setAttribute";
	private static final String HAS_ATTRIBUTE_METHOD_NAME = "hasAttribute";
	private static final String GET_ATTRIBUTE_METHOD_NAME = "getAttribute";
	private static final String GET_TEST_INSTANCE_METHOD = "getTestInstance";

	/**
	 * Set up a {@link StepExecution} as a test context attribute.
	 * 
	 * @param testContext the current test context
	 * @throws Exception if there is a problem
	 * @see TestExecutionListener#prepareTestInstance(TestContext)
	 */
	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		StepExecution stepExecution = getStepExecution(testContext);

		if (stepExecution != null) {
			Method method = TestContext.class.getMethod(SET_ATTRIBUTE_METHOD_NAME, String.class, Object.class);
			ReflectionUtils.invokeMethod(method, testContext, STEP_EXECUTION, stepExecution);
		}
	}

	/**
	 * @param testContext the current test context
	 * @throws Exception if there is a problem
	 * @see TestExecutionListener#beforeTestMethod(TestContext)
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		Method hasAttributeMethod = TestContext.class.getMethod(HAS_ATTRIBUTE_METHOD_NAME, String.class);
		Boolean hasAttribute = (Boolean) ReflectionUtils.invokeMethod(hasAttributeMethod, testContext, STEP_EXECUTION);

		if (hasAttribute) {
			Method method = TestContext.class.getMethod(GET_ATTRIBUTE_METHOD_NAME, String.class);
			StepExecution stepExecution = (StepExecution) ReflectionUtils.invokeMethod(method, testContext, STEP_EXECUTION);

			StepSynchronizationManager.register(stepExecution);
		}
	}

	/**
	 * @param testContext the current test context
	 * @throws Exception if there is a problem
	 * @see TestExecutionListener#afterTestMethod(TestContext)
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		Method method = TestContext.class.getMethod(HAS_ATTRIBUTE_METHOD_NAME, String.class);
		Boolean hasAttribute = (Boolean) ReflectionUtils.invokeMethod(method, testContext, STEP_EXECUTION);

		if (hasAttribute) {
			StepSynchronizationManager.close();
		}
	}

	/*
	 * Support for Spring 3.0 (empty).
	 */
	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
	}

	/*
	 * Support for Spring 3.0 (empty).
	 */
	@Override
	public void beforeTestClass(TestContext testContext) throws Exception {
	}
	
	/**
	 * Discover a {@link StepExecution} as a field in the test case or create
	 * one if none is available.
	 * 
	 * @param testContext the current test context
	 * @return a {@link StepExecution}
	 */
	protected StepExecution getStepExecution(TestContext testContext) {
		Object target;

		try {
			Method method = TestContext.class.getMethod(GET_TEST_INSTANCE_METHOD);
			target = ReflectionUtils.invokeMethod(method, testContext);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException("No such method " + GET_TEST_INSTANCE_METHOD + " on provided TestContext", e);
		}

		ExtractorMethodCallback method = new ExtractorMethodCallback(StepExecution.class, "getStepExecution");
		ReflectionUtils.doWithMethods(target.getClass(), method);
		if (method.getName() != null) {
			HippyMethodInvoker invoker = new HippyMethodInvoker();
			invoker.setTargetObject(target);
			invoker.setTargetMethod(method.getName());
			try {
				invoker.prepare();
				return (StepExecution) invoker.invoke();
			}
			catch (Exception e) {
				throw new IllegalArgumentException("Could not create step execution from method: " + method.getName(),
						e);
			}
		}

		return MetaDataInstanceFactory.createStepExecution();
	}

	/**
	 * Look for a method returning the type provided, preferring one with the
	 * name provided.
	 */
	private final class ExtractorMethodCallback implements MethodCallback {
		private String preferredName;

		private final Class<?> preferredType;

		private Method result;

		public ExtractorMethodCallback(Class<?> preferredType, String preferredName) {
			super();
			this.preferredType = preferredType;
			this.preferredName = preferredName;
		}

		public String getName() {
			return result == null ? null : result.getName();
		}

		@Override
		public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
			Class<?> type = method.getReturnType();
			if (preferredType.isAssignableFrom(type)) {
				if (result == null || method.getName().equals(preferredName)) {
					result = method;
				}
			}
		}
	}
}
