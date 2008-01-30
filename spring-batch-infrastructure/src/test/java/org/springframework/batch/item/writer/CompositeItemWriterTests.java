package org.springframework.batch.item.writer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.StreamContext;
import org.springframework.batch.item.stream.GenericStreamContext;
import org.springframework.batch.item.stream.ItemStreamAdapter;
import org.springframework.batch.statistics.StatisticsProvider;

/**
 * Tests for {@link CompositeItemWriter}
 * 
 * @author Robert Kasanicky
 */
public class CompositeItemWriterTests extends TestCase {

	// object under test
	private CompositeItemWriter itemProcessor = new CompositeItemWriter();
	
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
			MockControl control = MockControl.createStrictControl(ItemWriter.class);
			ItemWriter processor = (ItemWriter) control.getMock();
			
			processor.write(data);
			control.setVoidCallable();
			control.replay();
			
			processors.add(processor);
			controls.add(control);
		}
		
		itemProcessor.setItemWriters(processors);
		itemProcessor.write(data);
		
		for (Iterator iterator = controls.iterator(); iterator.hasNext();) {
			MockControl control = (MockControl) iterator.next();
			control.verify();
		}
	}
	
	/**
	 * All Restartable processors should be restarted, not-Restartable processors should be ignored.
	 */
	public void testRestart() {
		final ItemWriter p2 = new ItemWriterStub();
		final ItemWriter p3 = new ItemWriterStub();
		List itemProcessors = new ArrayList(){{
			add(p2);
			add(p3);
		}};
		itemProcessor.setItemWriters(itemProcessors);
		
		StreamContext rd = itemProcessor.getRestartData();
		itemProcessor.restoreFrom(rd);
		
		for (Iterator iterator = itemProcessors.iterator(); iterator.hasNext();) {
			ItemWriter processor = (ItemWriter) iterator.next();
			if (processor instanceof ItemWriterStub) {
				assertTrue("Injected processors are restarted", 
						((ItemWriterStub)processor).restarted);
			}
		}
		
	}
	
	public void testClose() throws Exception {
		
		final int NUMBER_OF_PROCESSORS = 10;
		
		List controls = new ArrayList(NUMBER_OF_PROCESSORS);
		List processors = new ArrayList(NUMBER_OF_PROCESSORS);
		
		for (int i = 0; i < NUMBER_OF_PROCESSORS; i++) {
			MockControl control = MockControl.createStrictControl(ItemWriter.class);
			ItemWriter processor = (ItemWriter) control.getMock();
			
			processor.close();
			control.setVoidCallable();
			control.replay();
			
			processors.add(processor);
			controls.add(control);
		}
		
		itemProcessor.setItemWriters(processors);
		itemProcessor.close();
		
		for (Iterator iterator = controls.iterator(); iterator.hasNext();) {
			MockControl control = (MockControl) iterator.next();
			control.verify();
		}
	}
	
	/**
	 * Stub for testing restart. Checks the restart data received is the same that was returned by
	 * <code>getRestartData()</code>
	 */
	private static class ItemWriterStub extends ItemStreamAdapter implements ItemWriter, StatisticsProvider {
		
		private static final String RESTART_KEY = "restartData";
		private static final String STATS_KEY = "stats";
		
		private boolean restarted = false;
		
		private final int hashCode = this.hashCode();
		
		
		public StreamContext getRestartData() {
			Properties props = new Properties(){{
				setProperty(RESTART_KEY, String.valueOf(hashCode));
			}};
			return new GenericStreamContext(props);
		}

		public void restoreFrom(StreamContext data) {
			if (Integer.valueOf(data.getProperties().getProperty(RESTART_KEY)).intValue() != hashCode()) {
				fail("received restart data is not the same which was saved");
			}
			restarted = true;
		}

		public void write(Object data) throws Exception {
			// do nothing
		}
		
		public Properties getStatistics() {
			return new Properties() {{
				setProperty(STATS_KEY, String.valueOf(hashCode));
			}};
		}

		public void close() throws Exception {
			
		}
		
	}

}
