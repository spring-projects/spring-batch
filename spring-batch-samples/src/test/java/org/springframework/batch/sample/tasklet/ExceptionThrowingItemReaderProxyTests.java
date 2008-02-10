package org.springframework.batch.sample.tasklet;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.item.reader.ListItemReader;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.synch.RepeatSynchronizationManager;

public class ExceptionThrowingItemReaderProxyTests extends TestCase {

	//expected call count before exception is thrown (exception should be thrown in next iteration)
	private static final int ITER_COUNT = 5;
	
	protected void tearDown() throws Exception {
		RepeatSynchronizationManager.clear();
	}
	
	public void testProcess() throws Exception {
				
		//create module and set item processor and iteration count
		ExceptionThrowingItemReaderProxy itemReader = new ExceptionThrowingItemReaderProxy(new ListItemReader(new ArrayList() {{
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
			} catch (BatchCriticalException bce) {
				assertEquals(ITER_COUNT,i);
			}
		}
		
	}
}
