package org.springframework.batch.sample.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Test;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.support.RepeatSynchronizationManager;
import org.springframework.batch.sample.support.ExceptionThrowingItemReaderProxy;

public class ExceptionThrowingItemReaderProxyTests {

	//expected call count before exception is thrown (exception should be thrown in next iteration)
	private static final int ITER_COUNT = 5;
	
	@After
	public void tearDown() throws Exception {
		RepeatSynchronizationManager.clear();
	}
	
	@Test
	public void testProcess() throws Exception {
				
		//create module and set item processor and iteration count
		ExceptionThrowingItemReaderProxy<String> itemReader = new ExceptionThrowingItemReaderProxy<String>();
		itemReader.setDelegate(new ListItemReader<String>(new ArrayList<String>() {{
			add("a");
			add("b");
			add("c");
			add("d");
			add("e");
			add("f");
		}}));

		itemReader.setThrowExceptionOnRecordNumber(ITER_COUNT + 1);
		
		RepeatSynchronizationManager.register(new RepeatContextSupport(null));
		
		//call process method multiple times and verify whether exception is thrown when expected
		for (int i = 0; i <= ITER_COUNT; i++) {
			try {
				itemReader.read();
				assertTrue(i < ITER_COUNT);
			} catch (UnexpectedJobExecutionException bce) {
				assertEquals(ITER_COUNT,i);
			}
		}
		
	}
}
