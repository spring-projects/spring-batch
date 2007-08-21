package org.springframework.batch.item.processor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.processor.CompositeItemProcessor;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.batch.statistics.StatisticsProvider;

/**
 * Tests for {@link CompositeItemProcessor}
 * 
 * @author Robert Kasanicky
 */
public class CompositeItemProcessorTests extends TestCase {

	// object under test
	private CompositeItemProcessor itemProcessor = new CompositeItemProcessor();
	
	/**
	 * Regular usage scenario.
	 * All injected processors should be called.
	 */
	public void testProcess() throws Exception {
		
		final int NUMBER_OF_PROCESSORS = 10;
		Object data = new Object();
		
		List controls = new ArrayList(NUMBER_OF_PROCESSORS);
		List processors = new ArrayList(NUMBER_OF_PROCESSORS);
		
		for (int i = 0; i < NUMBER_OF_PROCESSORS; i++) {
			MockControl control = MockControl.createStrictControl(ItemProcessor.class);
			ItemProcessor processor = (ItemProcessor) control.getMock();
			
			processor.process(data);
			control.setVoidCallable();
			control.replay();
			
			processors.add(processor);
			controls.add(control);
		}
		
		itemProcessor.setItemProcessors(processors);
		itemProcessor.process(data);
		
		for (Iterator iterator = controls.iterator(); iterator.hasNext();) {
			MockControl control = (MockControl) iterator.next();
			control.verify();
		}
	}
	
	/**
	 * Statistics of injected ItemProcessors should be returned under keys prefixed with their list index.
	 */
	public void testStatistics() {
		final ItemProcessor p1 = new ItemProcessorStub();
		final ItemProcessor p2 = new ItemProcessorStub();
		
		List itemProcessors = new ArrayList(){{
			add(p1); 
			add(p2);
		}};
		
		itemProcessor.setItemProcessors(itemProcessors);
		Properties stats = itemProcessor.getStatistics();
		assertEquals(String.valueOf(p1.hashCode()), stats.getProperty("0#" + ItemProcessorStub.STATS_KEY));
		assertEquals(String.valueOf(p2.hashCode()), stats.getProperty("1#" + ItemProcessorStub.STATS_KEY));
	}
	
	/**
	 * All Restartable processors should be restarted, not-Restartable processors should be ignored.
	 */
	public void testRestart() {
		//this mock with undefined behavior makes sure not-Restartable processor is ignored
		MockControl p1c = MockControl.createStrictControl(ItemProcessor.class);
		final ItemProcessor p1 = (ItemProcessor) p1c.getMock();
		
		final ItemProcessor p2 = new ItemProcessorStub();
		final ItemProcessor p3 = new ItemProcessorStub();
		List itemProcessors = new ArrayList(){{
			add(p1);
			add(p2);
			add(p3);
		}};
		itemProcessor.setItemProcessors(itemProcessors);
		
		RestartData rd = itemProcessor.getRestartData();
		itemProcessor.restoreFrom(rd);
		
		for (Iterator iterator = itemProcessors.iterator(); iterator.hasNext();) {
			ItemProcessor processor = (ItemProcessor) iterator.next();
			if (processor instanceof ItemProcessorStub) {
				assertTrue("Injected processors are restarted", 
						((ItemProcessorStub)processor).restarted);
			}
		}
		
	}
	
	/**
	 * Stub for testing restart. Checks the restart data received is the same that was returned by
	 * <code>getRestartData()</code>
	 */
	private static class ItemProcessorStub implements ItemProcessor, Restartable, StatisticsProvider {
		
		private static final String RESTART_KEY = "restartData";
		private static final String STATS_KEY = "stats";
		
		private boolean restarted = false;
		
		private final int hashCode = this.hashCode();
		
		
		public RestartData getRestartData() {
			Properties props = new Properties(){{
				setProperty(RESTART_KEY, String.valueOf(hashCode));
			}};
			return new GenericRestartData(props);
		}

		public void restoreFrom(RestartData data) {
			if (Integer.valueOf(data.getProperties().getProperty(RESTART_KEY)).intValue() != hashCode()) {
				fail("received restart data is not the same which was saved");
			}
			restarted = true;
		}

		public void process(Object data) throws Exception {
			// do nothing
		}
		
		public Properties getStatistics() {
			return new Properties() {{
				setProperty(STATS_KEY, String.valueOf(hashCode));
			}};
		}
		
	}

}
