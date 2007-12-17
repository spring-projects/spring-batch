package org.springframework.batch.sample.tasklet;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.provider.ListItemReader;
import org.springframework.batch.repeat.context.RepeatContextSupport;
import org.springframework.batch.repeat.synch.RepeatSynchronizationManager;
import org.springframework.batch.sample.tasklet.ExceptionRestartableTasklet;

public class ExceptionRestartableTaskletTests extends TestCase {

	//expected call count before exception is thrown (exception should be thrown in next iteration)
	private static final int ITER_COUNT = 5;
	
	protected void tearDown() throws Exception {
		RepeatSynchronizationManager.clear();
	}
	
	public void testProcess() throws Exception {
		
		//create mock item processor wich will be called by module.process() method
		MockControl processorControl = MockControl.createControl(ItemProcessor.class);
		ItemProcessor itemProcessor = (ItemProcessor)processorControl.getMock();
		
		//set expected call count and argument matcher 
		itemProcessor.process(null);
		processorControl.setMatcher(MockControl.ALWAYS_MATCHER);
		processorControl.setVoidCallable(ITER_COUNT);
		processorControl.replay();
		
		//create module and set item processor and iteration count
		ExceptionRestartableTasklet module = new ExceptionRestartableTasklet();
		module.setItemProcessor(itemProcessor);
		module.setThrowExceptionOnRecordNumber(ITER_COUNT + 1);
		
		module.setItemReader(new ListItemReader(new ArrayList() {{
			add("a");
			add("b");
			add("c");
			add("d");
			add("e");
			add("f");
		}}));
		
		RepeatSynchronizationManager.register(new RepeatContextSupport(null));
		
		//call process method multiple times and verify whether exception is thrown when expected
		for (int i = 0; i <= ITER_COUNT; i++) {
			try {
				module.execute();
				assertTrue(i < ITER_COUNT);
			} catch (BatchCriticalException bce) {
				assertEquals(ITER_COUNT,i);
			}
		}
		
		//verify method calls 
		processorControl.verify();
	}
}
