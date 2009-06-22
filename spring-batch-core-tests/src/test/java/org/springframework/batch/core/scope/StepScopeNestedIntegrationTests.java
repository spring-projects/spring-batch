package org.springframework.batch.core.scope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class StepScopeNestedIntegrationTests implements BeanFactoryAware {
	
	@Autowired
	@Qualifier("step1")
	private Step step1;	
		
	@Autowired
	@Qualifier("step2")
	private Step step2;	
		
	@Autowired
	@Qualifier("step3")
	private Step step3;	
		
	@Autowired
	@Qualifier("parent")
	private Collaborator parent;	
	
	private StepExecution stepExecution;

	private ConfigurableListableBeanFactory beanFactory;

	private List<String> names;
	
	/**
	 * {@inheritDoc}
	 */
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	@Before
	public void setUp() {
		TestStep.reset();
		stepExecution = new StepExecution("step", new JobExecution(11L), 12L);
		names = Arrays.asList(beanFactory.getBeanDefinitionNames());
	}
		
	@Test
	public void testNestedScopedProxy() throws Exception {
		assertNotNull(step1);
		assertEquals("foo", parent.getName());
		assertTrue("Proxy was not created for explicitly scoped bean", names.contains("lazyBindingProxy.bar"));
		assertFalse("Proxy was created for unscoped bean", names.contains("foo"));
		stepExecution.getExecutionContext().putString("bar", "bar");
		step1.execute(stepExecution);
		assertNotNull(stepExecution.getExecutionContext().getString("foo"));
		assertEquals("bar", TestStep.getContext().getAttribute("collaborator"));
		assertEquals("foo", TestStep.getContext().getAttribute("parent"));
	}

	@Test
	public void testNestedUnScoped() throws Exception {
		assertNotNull(step2);
		assertTrue("Proxy was not created for explicitly scoped bean", names.contains("lazyBindingProxy.spam"));
		assertFalse("Proxy was created for unscoped bean", names.contains("bucket"));
		stepExecution.getExecutionContext().putString("spam", "spam");
		step2.execute(stepExecution);
		assertNotNull(stepExecution.getExecutionContext().getString("foo"));
		assertEquals("spam", TestStep.getContext().getAttribute("collaborator"));
		assertEquals("bucket", TestStep.getContext().getAttribute("parent"));
	}

	@Test
	public void testTwiceNestedScopedProxy() throws Exception {
		assertNotNull(step3);
		assertTrue("Proxy was not created for explicitly scoped bean", names.contains("lazyBindingProxy.maps"));
		assertFalse("Proxy was created for implicitly scoped bean", names.contains("lazyBindingProxy.rab"));
		assertEquals("foo", parent.getName());
		stepExecution.getExecutionContext().putString("maps", "maps");
		stepExecution.getExecutionContext().putString("rab", "rab");
		step3.execute(stepExecution);
		assertNotNull(stepExecution.getExecutionContext().getString("foo"));
		assertEquals("maps", TestStep.getContext().getAttribute("collaborator"));
		assertEquals("rab", TestStep.getContext().getAttribute("parent"));
	}

}
