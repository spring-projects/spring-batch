package org.springframework.batch.sample.item.reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.MarkFailedException;
import org.springframework.batch.item.ResetFailedException;


/**
 * Creates items infinitely - useful for testing job interruption by user.
 */
public class InfiniteItemReader implements ItemReader {

	private static final Log logger = LogFactory.getLog(InfiniteItemReader.class);
	
	private long counter = 0;
	
	public Object read() throws Exception {
		Object item = "item" + counter;
		Thread.sleep(10);
		logger.info("read item: " + item);
		return item;
	}

	public void mark() throws MarkFailedException {
		// no-op
		
	}

	public void reset() throws ResetFailedException {
		// no-op
		
	}

	

}
