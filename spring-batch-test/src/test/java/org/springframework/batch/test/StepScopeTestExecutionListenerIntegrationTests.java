package org.springframework.batch.test;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
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
@TestExecutionListeners( { DependencyInjectionTestExecutionListener.class, StepScopeTestExecutionListener.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class StepScopeTestExecutionListenerIntegrationTests {

	@Autowired
	private ItemReader<String> reader;

	@Autowired
	private ItemStream stream;

	protected Map<String, Object> executionContext;

	public StepScopeTestExecutionListenerIntegrationTests() {
		executionContext = Collections.singletonMap("input.file",
				(Object) "classpath:/org/springframework/batch/test/simple.txt");
	}

	@Test
	public void testJob() throws Exception {
		stream.open(new ExecutionContext());
		assertEquals("foo", reader.read());
	}

}
