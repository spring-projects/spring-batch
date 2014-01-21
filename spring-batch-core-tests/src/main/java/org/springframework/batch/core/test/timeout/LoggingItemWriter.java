package org.springframework.batch.core.test.timeout;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;

public class LoggingItemWriter<T> implements ItemWriter<T> {
	
	protected Log logger = LogFactory.getLog(LoggingItemWriter.class);

	@Override
	public void write(List<? extends T> items) throws Exception {
		logger.info(items);		
	}

}
