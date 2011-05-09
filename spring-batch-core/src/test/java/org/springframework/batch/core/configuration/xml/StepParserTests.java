/*
 * Copyright 2006-2009 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.listener.CompositeStepExecutionListener;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.SimpleJobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.core.step.item.FatalSkippableException;
import org.springframework.batch.core.step.item.FatalRuntimeException;
import org.springframework.batch.core.step.item.ForceRollbackForWriteSkipException;
import org.springframework.batch.core.step.item.SkippableException;
import org.springframework.batch.core.step.item.SkippableRuntimeException;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.support.CompositeItemStream;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.retry.RetryListener;
import org.springframework.batch.retry.listener.RetryListenerSupport;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

/**
 * @author Thomas Risberg
 * @author Dan Garrette
 */
public class StepParserTests {

	private static ApplicationContext stepParserParentAttributeTestsCtx;

	@BeforeClass
	public static void loadAppCtx() {
		stepParserParentAttributeTestsCtx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserParentAttributeTests-context.xml");
	}

	@Test
	public void testTaskletStepAttributes() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserTaskletAttributesTests-context.xml");
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Map<String, StepParserStepFactoryBean> beans = ctx.getBeansOfType(StepParserStepFactoryBean.class);
		String factoryName = (String) beans.keySet().toArray()[0];
		@SuppressWarnings("unchecked")
		StepParserStepFactoryBean<Object, Object> factory = beans.get(factoryName);
		TaskletStep bean = (TaskletStep) factory.getObject();
		assertEquals("wrong start-limit:", 25, bean.getStartLimit());
		Object throttleLimit = ReflectionTestUtils.getField(factory, "throttleLimit");
		assertEquals(new Integer(10), throttleLimit);
	}

	@Test
	public void testStepParserBeanName() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserBeanNameTests-context.xml");
		@SuppressWarnings("unchecked")
		Map<String, Step> beans = ctx.getBeansOfType(Step.class);
		assertTrue("'s1' bean not found", beans.containsKey("s1"));
		Step s1 = (Step) ctx.getBean("s1");
		assertEquals("wrong name", "s1", s1.getName());
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void testStepParserCommitIntervalCompletionPolicy() throws Exception {
		new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserCommitIntervalCompletionPolicyTests-context.xml");
	}

	@Test
	public void testStepParserCommitInterval() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserCommitIntervalTests-context.xml");
		@SuppressWarnings("unchecked")
		Map<String, Step> beans = ctx.getBeansOfType(Step.class);
		assertTrue("'s1' bean not found", beans.containsKey("s1"));
		Step s1 = (Step) ctx.getBean("s1");
		CompletionPolicy completionPolicy = getCompletionPolicy(s1);
		assertTrue(completionPolicy instanceof SimpleCompletionPolicy);
		assertEquals(25, ReflectionTestUtils.getField(completionPolicy, "chunkSize"));
	}

	@Test
	public void testStepParserCompletionPolicy() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserCompletionPolicyTests-context.xml");
		@SuppressWarnings("unchecked")
		Map<String, Step> beans = ctx.getBeansOfType(Step.class);
		assertTrue("'s1' bean not found", beans.containsKey("s1"));
		Step s1 = (Step) ctx.getBean("s1");
		CompletionPolicy completionPolicy = getCompletionPolicy(s1);
		assertTrue(completionPolicy instanceof DummyCompletionPolicy);
	}

	private CompletionPolicy getCompletionPolicy(Step s1) throws NoSuchFieldException, IllegalAccessException {
		Object tasklet = ReflectionTestUtils.getField(s1, "tasklet");
		Object chunkProvider = ReflectionTestUtils.getField(tasklet, "chunkProvider");
		Object repeatOperations = ReflectionTestUtils.getField(chunkProvider, "repeatOperations");
		return (CompletionPolicy) ReflectionTestUtils.getField(repeatOperations, "completionPolicy");
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void testStepParserNoCommitIntervalOrCompletionPolicy() throws Exception {
		new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/StepParserNoCommitIntervalOrCompletionPolicyTests-context.xml");
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void testTaskletStepWithBadStepListener() throws Exception {
		String contextLocation = "org/springframework/batch/core/configuration/xml/StepParserBadStepListenerTests-context.xml";
		new XmlBeanFactory(new ClassPathResource(contextLocation));
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void testTaskletStepWithBadRetryListener() throws Exception {
		String contextLocation = "org/springframework/batch/core/configuration/xml/StepParserBadRetryListenerTests-context.xml";
		new XmlBeanFactory(new ClassPathResource(contextLocation));
	}

	@Test
	public void testParentStep() throws Exception {
		ApplicationContext ctx = stepParserParentAttributeTestsCtx;

		// Inline Step
		assertTrue(getListener("s1", ctx) instanceof StepExecutionListenerSupport);

		// Standalone Step
		assertTrue(getListener("s2", ctx) instanceof StepExecutionListenerSupport);

		// Inline With Tasklet Attribute Step
		assertTrue(getListener("s3", ctx) instanceof StepExecutionListenerSupport);

		// Standalone With Tasklet Attribute Step
		assertTrue(getListener("s4", ctx) instanceof StepExecutionListenerSupport);
	}

	@Test
	public void testInheritTransactionAttributes() throws Exception {
		ApplicationContext ctx = stepParserParentAttributeTestsCtx;

		// On Inline - No Merge
		validateTransactionAttributesInherited("s1", ctx);

		// On Standalone - No Merge
		validateTransactionAttributesInherited("s2", ctx);

		// On Inline With Tasklet Ref - No Merge
		validateTransactionAttributesInherited("s3", ctx);

		// On Standalone With Tasklet Ref - No Merge
		validateTransactionAttributesInherited("s4", ctx);

		// On Inline
		validateTransactionAttributesInherited("s5", ctx);

		// On Standalone
		validateTransactionAttributesInherited("s6", ctx);

		// On Inline With Tasklet Ref
		validateTransactionAttributesInherited("s7", ctx);

		// On Standalone With Tasklet Ref
		validateTransactionAttributesInherited("s8", ctx);
	}

	private void validateTransactionAttributesInherited(String stepName, ApplicationContext ctx) {
		DefaultTransactionAttribute txa = getTransactionAttribute(ctx, stepName);
		assertEquals(TransactionDefinition.PROPAGATION_REQUIRED, txa.getPropagationBehavior());
		assertEquals(TransactionDefinition.ISOLATION_DEFAULT, txa.getIsolationLevel());
		assertEquals(10, txa.getTimeout());
	}

	@SuppressWarnings("unchecked")
	private List<StepExecutionListener> getListeners(String stepName, ApplicationContext ctx) throws Exception {
		assertTrue(ctx.containsBean(stepName));
		Step step = (Step) ctx.getBean(stepName);
		assertTrue(step instanceof TaskletStep);
		Object compositeListener = ReflectionTestUtils.getField(step, "stepExecutionListener");
		Object composite = ReflectionTestUtils.getField(compositeListener, "list");
		List<StepExecutionListener> list = (List<StepExecutionListener>) ReflectionTestUtils
				.getField(composite, "list");
		List<StepExecutionListener> unwrappedList = new ArrayList<StepExecutionListener>();
		for (StepExecutionListener listener : list) {
			while (listener instanceof Advised) {
				listener = (StepExecutionListener) ((Advised) listener).getTargetSource().getTarget();
			}
			unwrappedList.add(listener);
		}
		return unwrappedList;
	}

	private StepExecutionListener getListener(String stepName, ApplicationContext ctx) throws Exception {
		List<StepExecutionListener> list = getListeners(stepName, ctx);
		assertEquals(1, list.size());
		return list.get(0);
	}

	private DefaultTransactionAttribute getTransactionAttribute(ApplicationContext ctx, String stepName) {
		assertTrue(ctx.containsBean(stepName));
		Step step = (Step) ctx.getBean(stepName);
		assertTrue(step instanceof TaskletStep);
		Object transactionAttribute = ReflectionTestUtils.getField(step, "transactionAttribute");
		return (DefaultTransactionAttribute) transactionAttribute;
	}

	@Test
	public void testInheritFromBean() throws Exception {
		ApplicationContext ctx = stepParserParentAttributeTestsCtx;

		assertTrue(getTasklet("s9", ctx) instanceof DummyTasklet);
		assertTrue(getTasklet("s10", ctx) instanceof DummyTasklet);
	}

	private Tasklet getTasklet(String stepName, ApplicationContext ctx) {
		assertTrue(ctx.containsBean(stepName));
		Step step = (Step) ctx.getBean(stepName);
		assertTrue(step instanceof TaskletStep);
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		assertTrue(tasklet instanceof Tasklet);
		return (Tasklet) tasklet;
	}

	@Test
	public void testJobRepositoryDefaults() throws Exception {
		ApplicationContext ctx = stepParserParentAttributeTestsCtx;

		assertTrue(getJobRepository("defaultRepoStep", ctx) instanceof SimpleJobRepository);

		assertTrue(getJobRepository("defaultRepoStepWithParent", ctx) instanceof SimpleJobRepository);

		assertTrue(getJobRepository("overrideRepoStep", ctx) instanceof SimpleJobRepository);

		assertDummyJobRepository("injectedRepoStep", "dummyJobRepository", ctx);

		assertDummyJobRepository("injectedRepoStepWithParent", "dummyJobRepository", ctx);

		assertDummyJobRepository("injectedOverrideRepoStep", "dummyJobRepository", ctx);

		assertDummyJobRepository("injectedRepoFromParentStep", "dummyJobRepository2", ctx);

		assertDummyJobRepository("injectedRepoFromParentStepWithParent", "dummyJobRepository2", ctx);

		assertDummyJobRepository("injectedOverrideRepoFromParentStep", "dummyJobRepository2", ctx);

		assertTrue(getJobRepository("defaultRepoStandaloneStep", ctx) instanceof SimpleJobRepository);

		assertDummyJobRepository("specifiedRepoStandaloneStep", "dummyJobRepository2", ctx);
	}

	@Test
	public void testTransactionManagerDefaults() throws Exception {
		ApplicationContext ctx = stepParserParentAttributeTestsCtx;

		assertTrue(getTransactionManager("defaultTxMgrStep", ctx) instanceof ResourcelessTransactionManager);

		assertDummyTransactionManager("specifiedTxMgrStep", "dummyTxMgr", ctx);

		assertDummyTransactionManager("defaultTxMgrWithParentStep", "dummyTxMgr", ctx);

		assertDummyTransactionManager("overrideTxMgrOnParentStep", "dummyTxMgr2", ctx);
	}

	private void assertDummyJobRepository(String beanName, String jobRepoName, ApplicationContext ctx) throws Exception {
		JobRepository jobRepository = getJobRepository(beanName, ctx);
		assertTrue(jobRepository instanceof DummyJobRepository);
		assertEquals(jobRepoName, ((DummyJobRepository) jobRepository).getName());
	}

	private void assertDummyTransactionManager(String beanName, String txMgrName, ApplicationContext ctx)
			throws Exception {
		PlatformTransactionManager txMgr = getTransactionManager(beanName, ctx);
		assertTrue(txMgr instanceof DummyPlatformTransactionManager);
		assertEquals(txMgrName, ((DummyPlatformTransactionManager) txMgr).getName());
	}

	private JobRepository getJobRepository(String beanName, ApplicationContext ctx) throws Exception {
		Object jobRepository = getFieldFromBean(beanName, "jobRepository", ctx);
		assertTrue(jobRepository instanceof JobRepository);
		return (JobRepository) jobRepository;
	}

	private PlatformTransactionManager getTransactionManager(String beanName, ApplicationContext ctx) throws Exception {
		Object jobRepository = getFieldFromBean(beanName, "transactionManager", ctx);
		assertTrue(jobRepository instanceof PlatformTransactionManager);
		return (PlatformTransactionManager) jobRepository;
	}

	private Object getFieldFromBean(String beanName, String field, ApplicationContext ctx) throws Exception {
		assertTrue(ctx.containsBean(beanName));
		Object bean = ctx.getBean(beanName);
		assertTrue(bean instanceof AbstractStep || bean instanceof AbstractJob);
		Object property = ReflectionTestUtils.getField(bean, field);
		while (property instanceof Advised) {
			property = ((Advised) property).getTargetSource().getTarget();
		}
		return property;
	}

	@Test
	public void testNonAbstractStep() {
		ApplicationContext ctx = stepParserParentAttributeTestsCtx;

		assertTrue(ctx.containsBean("s11"));
		Object bean = ctx.getBean("s11");
		assertTrue(bean instanceof DummyStep);
	}

	@Test
	public void testInlineTaskletElementOverridesParentBeanClass() {
		ApplicationContext ctx = stepParserParentAttributeTestsCtx;

		assertTrue(ctx.containsBean("&s12"));
		Object factoryBean = ctx.getBean("&s12");
		assertTrue(factoryBean instanceof StepParserStepFactoryBean<?,?>);

		assertTrue(ctx.containsBean("dummyStep"));
		Object dummyStep = ctx.getBean("dummyStep");
		assertTrue(dummyStep instanceof DummyStep);

		assertTrue(ctx.containsBean("s12"));
		Object bean = ctx.getBean("s12");
		assertTrue(bean instanceof TaskletStep);
	}

	@Test
	public void testTaskletElementOverridesChildBeanClass() {
		ApplicationContext ctx = stepParserParentAttributeTestsCtx;

		assertTrue(ctx.containsBean("&s13"));
		Object factoryBean = ctx.getBean("&s13");
		assertTrue(factoryBean instanceof StepParserStepFactoryBean<?,?>);

		assertTrue(ctx.containsBean("s13"));
		Object bean = ctx.getBean("s13");
		assertTrue(bean instanceof TaskletStep);

		assertTrue(ctx.containsBean("&dummyStepWithTaskletOnParent"));
		Object dummyStepFb = ctx.getBean("&dummyStepWithTaskletOnParent");
		assertTrue(dummyStepFb instanceof StepParserStepFactoryBean<?,?>);

		assertTrue(ctx.containsBean("dummyStepWithTaskletOnParent"));
		Object dummyStep = ctx.getBean("dummyStepWithTaskletOnParent");
		assertTrue(dummyStep instanceof TaskletStep);

		assertTrue(ctx.containsBean("&standaloneStepWithTasklet"));
		Object standaloneStepFb = ctx.getBean("&standaloneStepWithTasklet");
		assertTrue(standaloneStepFb instanceof StepParserStepFactoryBean<?,?>);

		assertTrue(ctx.containsBean("standaloneStepWithTasklet"));
		Object standaloneStep = ctx.getBean("standaloneStepWithTasklet");
		assertTrue(standaloneStep instanceof TaskletStep);
	}

	@Test
	public void testTaskletElementOverridesParentBeanClass() {
		ApplicationContext ctx = stepParserParentAttributeTestsCtx;

		assertTrue(ctx.containsBean("&s14"));
		Object factoryBean = ctx.getBean("&s14");
		assertTrue(factoryBean instanceof StepParserStepFactoryBean<?,?>);

		assertTrue(ctx.containsBean("s12"));
		Object bean = ctx.getBean("s12");
		assertTrue(bean instanceof TaskletStep);

		assertTrue(ctx.containsBean("&standaloneStepWithTaskletAndDummyParent"));
		Object standaloneWithTaskletFb = ctx.getBean("&standaloneStepWithTaskletAndDummyParent");
		assertTrue(standaloneWithTaskletFb instanceof StepParserStepFactoryBean<?,?>);

		assertTrue(ctx.containsBean("standaloneStepWithTaskletAndDummyParent"));
		Object standaloneWithTasklet = ctx.getBean("standaloneStepWithTaskletAndDummyParent");
		assertTrue(standaloneWithTasklet instanceof TaskletStep);

		assertTrue(ctx.containsBean("dummyStep"));
		Object dummyStep = ctx.getBean("dummyStep");
		assertTrue(dummyStep instanceof DummyStep);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testStepWithListsMerge() throws Exception {
		ApplicationContext ctx = stepParserParentAttributeTestsCtx;

		Map<Class<? extends Throwable>, Boolean> skippable = new HashMap<Class<? extends Throwable>, Boolean>();
		skippable.put(SkippableRuntimeException.class, true);
		skippable.put(SkippableException.class, true);
		skippable.put(FatalRuntimeException.class, false);
		skippable.put(FatalSkippableException.class, false);
		skippable.put(ForceRollbackForWriteSkipException.class, true);
		Map<Class<? extends Throwable>, Boolean> retryable = new HashMap<Class<? extends Throwable>, Boolean>();
		retryable.put(DeadlockLoserDataAccessException.class, true);
		retryable.put(FatalSkippableException.class, true);
		retryable.put(ForceRollbackForWriteSkipException.class, true);
		List<Class<? extends ItemStream>> streams = Arrays.asList(CompositeItemStream.class, TestReader.class);
		List<Class<? extends RetryListener>> retryListeners = Arrays.asList(RetryListenerSupport.class,
				DummyRetryListener.class);
		List<Class<? extends StepExecutionListener>> stepListeners = Arrays.asList(StepExecutionListenerSupport.class,
				CompositeStepExecutionListener.class);
		List<Class<? extends SkippableRuntimeException>> noRollback = Arrays.asList(FatalRuntimeException.class,
				SkippableRuntimeException.class);

		StepParserStepFactoryBean<?, ?> fb = (StepParserStepFactoryBean<?, ?>) ctx.getBean("&stepWithListsMerge");

		Map<Class<? extends Throwable>, Boolean> skippableFound = getExceptionMap(fb, "skippableExceptionClasses");
		Map<Class<? extends Throwable>, Boolean> retryableFound = getExceptionMap(fb, "retryableExceptionClasses");
		ItemStream[] streamsFound = (ItemStream[]) ReflectionTestUtils.getField(fb, "streams");
		RetryListener[] retryListenersFound = (RetryListener[]) ReflectionTestUtils.getField(fb, "retryListeners");
		StepListener[] stepListenersFound = (StepListener[]) ReflectionTestUtils.getField(fb, "listeners");
		Collection<Class<? extends Throwable>> noRollbackFound = getExceptionList(fb, "noRollbackExceptionClasses");

		assertSameMaps(skippable, skippableFound);
		assertSameMaps(retryable, retryableFound);
		assertSameCollections(streams, toClassCollection(streamsFound));
		assertSameCollections(retryListeners, toClassCollection(retryListenersFound));
		assertSameCollections(stepListeners, toClassCollection(stepListenersFound));
		assertSameCollections(noRollback, noRollbackFound);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testStepWithListsNoMerge() throws Exception {
		ApplicationContext ctx = stepParserParentAttributeTestsCtx;

		Map<Class<? extends Throwable>, Boolean> skippable = new HashMap<Class<? extends Throwable>, Boolean>();
		skippable.put(SkippableException.class, true);
		skippable.put(FatalSkippableException.class, false);
		skippable.put(ForceRollbackForWriteSkipException.class, true);
		Map<Class<? extends Throwable>, Boolean> retryable = new HashMap<Class<? extends Throwable>, Boolean>();
		retryable.put(FatalSkippableException.class, true);
		retryable.put(ForceRollbackForWriteSkipException.class, true);
		List<Class<CompositeItemStream>> streams = Arrays.asList(CompositeItemStream.class);
		List<Class<DummyRetryListener>> retryListeners = Arrays.asList(DummyRetryListener.class);
		List<Class<CompositeStepExecutionListener>> stepListeners = Arrays.asList(CompositeStepExecutionListener.class);
		List<Class<SkippableRuntimeException>> noRollback = Arrays.asList(SkippableRuntimeException.class);

		StepParserStepFactoryBean<?, ?> fb = (StepParserStepFactoryBean<?, ?>) ctx.getBean("&stepWithListsNoMerge");

		Map<Class<? extends Throwable>, Boolean> skippableFound = getExceptionMap(fb, "skippableExceptionClasses");
		Map<Class<? extends Throwable>, Boolean> retryableFound = getExceptionMap(fb, "retryableExceptionClasses");
		ItemStream[] streamsFound = (ItemStream[]) ReflectionTestUtils.getField(fb, "streams");
		RetryListener[] retryListenersFound = (RetryListener[]) ReflectionTestUtils.getField(fb, "retryListeners");
		StepListener[] stepListenersFound = (StepListener[]) ReflectionTestUtils.getField(fb, "listeners");
		Collection<Class<? extends Throwable>> noRollbackFound = getExceptionList(fb, "noRollbackExceptionClasses");

		assertSameMaps(skippable, skippableFound);
		assertSameMaps(retryable, retryableFound);
		assertSameCollections(streams, toClassCollection(streamsFound));
		assertSameCollections(retryListeners, toClassCollection(retryListenersFound));
		assertSameCollections(stepListeners, toClassCollection(stepListenersFound));
		assertSameCollections(noRollback, noRollbackFound);
	}

	@Test
	public void testStepWithListsOverrideWithEmpty() throws Exception {
		ApplicationContext ctx = stepParserParentAttributeTestsCtx;

		StepParserStepFactoryBean<?, ?> fb = (StepParserStepFactoryBean<?, ?>) ctx
				.getBean("&stepWithListsOverrideWithEmpty");

		assertEquals(1, getExceptionMap(fb, "skippableExceptionClasses").size());
		assertEquals(1, getExceptionMap(fb, "retryableExceptionClasses").size());
		assertEquals(0, ((ItemStream[]) ReflectionTestUtils.getField(fb, "streams")).length);
		assertEquals(0, ((RetryListener[]) ReflectionTestUtils.getField(fb, "retryListeners")).length);
		assertEquals(0, ((StepListener[]) ReflectionTestUtils.getField(fb, "listeners")).length);
		assertEquals(0, getExceptionList(fb, "noRollbackExceptionClasses").size());
	}

	@SuppressWarnings("unchecked")
	private Collection<Class<? extends Throwable>> getExceptionList(StepParserStepFactoryBean<?, ?> fb,
			String propertyName) {
		return (Collection<Class<? extends Throwable>>) ReflectionTestUtils.getField(fb, propertyName);
	}

	@SuppressWarnings("unchecked")
	private Map<Class<? extends Throwable>, Boolean> getExceptionMap(StepParserStepFactoryBean<?, ?> fb,
			String propertyName) {
		return (Map<Class<? extends Throwable>, Boolean>) ReflectionTestUtils.getField(fb, propertyName);
	}

	private <T, S extends T> void assertSameCollections(Collection<S> expected, Collection<T> actual) {
		assertEquals(expected.size(), actual.size());
		assertTrue(expected.containsAll(actual));
	}

	private <T, S> void assertSameMaps(Map<T, S> expected, Map<T, S> actual) {
		assertEquals(expected.size(), actual.size());
		for (Entry<T, S> e : expected.entrySet()) {
			assertTrue(actual.containsKey(e.getKey()));
			assertEquals(e.getValue(), actual.get(e.getKey()));
		}
	}

	private <T> Collection<Class<? extends T>> toClassCollection(T[] in) throws Exception {
		return toClassCollection(Arrays.asList(in));
	}

	@SuppressWarnings("unchecked")
	private <T> Collection<Class<? extends T>> toClassCollection(Collection<T> in) throws Exception {
		Collection<Class<? extends T>> out = new ArrayList<Class<? extends T>>();
		for (T item : in) {
			while (item instanceof Advised) {
				item = (T) ((Advised) item).getTargetSource().getTarget();
			}
			Class<? extends T> cls = (Class<? extends T>) item.getClass();
			out.add(cls);
		}
		return out;
	}
}
