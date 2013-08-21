package org.springframework.batch.core.jsr.configuration.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.batch.api.Decider;
import javax.batch.runtime.StepExecution;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.jsr.step.DecisionStep;

public class DecisionStepFactoryBeanTests {

	private DecisionStepFactoryBean factoryBean;

	@Before
	public void setUp() throws Exception {
		factoryBean = new DecisionStepFactoryBean();
	}

	@Test
	public void testGetObjectType() {
		assertEquals(DecisionStep.class, factoryBean.getObjectType());
	}

	@Test
	public void testIsSingleton() {
		assertTrue(factoryBean.isSingleton());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullDeciderAndName() throws Exception {
		factoryBean.afterPropertiesSet();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullDecider() throws Exception{
		factoryBean.setName("state1");
		factoryBean.afterPropertiesSet();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNullName() throws Exception {
		factoryBean.setDecider(new DeciderSupport());
		factoryBean.afterPropertiesSet();
	}

	@Test
	public void testDeciderDeciderState() throws Exception {
		factoryBean.setDecider(new DeciderSupport());
		factoryBean.setName("IL");

		factoryBean.afterPropertiesSet();

		Step step = factoryBean.getObject();

		assertEquals("IL", step.getName());
		assertEquals(DecisionStep.class, step.getClass());
	}

	public static class DeciderSupport implements Decider {

		@Override
		public String decide(StepExecution[] executions) throws Exception {
			return null;
		}
	}
}
