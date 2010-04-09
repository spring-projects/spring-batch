package org.springframework.batch.core.scope;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class StepScopePlaceholderIntegrationTests implements BeanFactoryAware {

	@Autowired
	@Qualifier("simple")
	private Collaborator simple;

	@Autowired
	@Qualifier("compound")
	private Collaborator compound;

	@Autowired
	@Qualifier("value")
	private Collaborator value;

	@Autowired
	@Qualifier("ref")
	private Collaborator ref;

	@Autowired
	@Qualifier("scopedRef")
	private Collaborator scopedRef;

	@Autowired
	@Qualifier("list")
	private Collaborator list;

	@Autowired
	@Qualifier("bar")
	private Collaborator bar;

	@Autowired
	@Qualifier("nested")
	private Collaborator nested;

	private StepExecution stepExecution;

	private ListableBeanFactory beanFactory;

	private int beanCount;

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@Before
	public void start() {
		start("bar");
	}

	private void start(String foo) {

		StepSynchronizationManager.close();
		stepExecution = new StepExecution("foo", new JobExecution(11L), 123L);

		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put("foo", foo);
		executionContext.put("parent", bar);

		stepExecution.setExecutionContext(executionContext);
		StepSynchronizationManager.register(stepExecution);

		beanCount = beanFactory.getBeanDefinitionCount();

	}

	@After
	public void stop() {
		StepSynchronizationManager.close();
		// Check that all temporary bean definitions are cleaned up
		assertEquals(beanCount, beanFactory.getBeanDefinitionCount());
	}

	@Test
	public void testSimpleProperty() throws Exception {
		assertEquals("bar", simple.getName());
		// Once the step context is set up it should be baked into the proxies
		// so changing it now should have no effect
		stepExecution.getExecutionContext().put("foo", "wrong!");
		assertEquals("bar", simple.getName());
	}

	@Test
	public void testCompoundProperty() throws Exception {
		assertEquals("bar-bar", compound.getName());
	}

	@Test
	public void testCompoundPropertyTwice() throws Exception {

		assertEquals("bar-bar", compound.getName());

		StepSynchronizationManager.close();
		stepExecution = new StepExecution("foo", new JobExecution(11L), 123L);

		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put("foo", "spam");

		stepExecution.setExecutionContext(executionContext);
		StepSynchronizationManager.register(stepExecution);

		assertEquals("spam-bar", compound.getName());

	}

	@Test
	public void testParentByRef() throws Exception {
		assertEquals("bar", ref.getParent().getName());
	}

	@Test
	public void testParentByValue() throws Exception {
		assertEquals("bar", value.getParent().getName());
	}

	@Test
	public void testList() throws Exception {
		assertEquals("[bar]", list.getList().toString());
	}

	@Test
	public void testNested() throws Exception {
		assertEquals("bar", nested.getParent().getName());
	}

	@Test
	public void testScopedRef() throws Exception {
		assertEquals("bar", scopedRef.getParent().getName());
		stop();
		start("spam");
		assertEquals("spam", scopedRef.getParent().getName());
	}

}
