package org.springframework.batch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * @author Dave Syer
 * @since 2.1
 */
@ContextConfiguration
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, JobScopeTestExecutionListener.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class JobScopeTestExecutionListenerIntegrationTests {

	@Autowired
	private ItemReader<String> reader;

	@Autowired
	private ItemStream stream;

	public JobExecution getJobExection() {
		// Assert that dependencies are already injected...
		assertNotNull(reader);
		// Then create the execution for the job scope...
		JobExecution execution = MetaDataInstanceFactory.createJobExecution();
		execution.getExecutionContext().putString("input.file", "classpath:/org/springframework/batch/test/simple.txt");
		return execution;
	}

	@Test
	public void testJob() throws Exception {
		stream.open(new ExecutionContext());
		assertEquals("foo", reader.read());
	}

}
