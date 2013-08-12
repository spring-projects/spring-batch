package org.springframework.batch.core.jsr.configuration.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.batch.api.Decider;
import javax.batch.runtime.StepExecution;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.job.flow.State;
import org.springframework.batch.core.job.flow.support.state.DecisionState;

public class DecisionStateFactoryBeanTests {

	private DecisionStateFactoryBean factoryBean;

	@Before
	public void setUp() throws Exception {
		factoryBean = new DecisionStateFactoryBean();
	}

	@Test
	public void testGetObjectType() {
		assertEquals(JobExecutionDecider.class, factoryBean.getObjectType());
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

	@Test(expected=IllegalArgumentException.class)
	public void setWrongDeciderType() {
		factoryBean.setDecider("Some decider");
	}

	@Test
	public void testJobExecutionDeciderState() throws Exception {
		factoryBean.setDecider(new JobExecutionDeciderSupport());
		factoryBean.setName("IL");

		factoryBean.afterPropertiesSet();

		State state = factoryBean.getObject();

		assertEquals("IL", state.getName());
		assertEquals(DecisionState.class, state.getClass());
	}

	@Test
	public void testDeciderDeciderState() throws Exception {
		factoryBean.setDecider(new DeciderSupport());
		factoryBean.setName("IL");

		factoryBean.afterPropertiesSet();

		State state = factoryBean.getObject();

		assertEquals("IL", state.getName());
		assertEquals(DecisionState.class, state.getClass());
	}

	public static class DeciderSupport implements Decider {

		@Override
		public String decide(StepExecution[] executions) throws Exception {
			return null;
		}
	}

	public static class JobExecutionDeciderSupport implements JobExecutionDecider {

		@Override
		public FlowExecutionStatus decide(JobExecution jobExecution,
				org.springframework.batch.core.StepExecution stepExecution) {
			return null;
		}
	}
}
