package org.springframework.batch.core.configuration.xml;

import static org.junit.Assert.assertTrue;

import javax.annotation.Resource;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

//@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class TaskletStepAllowStartIfCompleteTest {

	@Resource
	private ApplicationContext context;
	
	@Test
	public void test() throws Exception {
		//retrieve the step from the context and see that it's allow is set
		AbstractStep abstractStep = context.getBean(AbstractStep.class);
		assertTrue(abstractStep.isAllowStartIfComplete());	
	}

}
