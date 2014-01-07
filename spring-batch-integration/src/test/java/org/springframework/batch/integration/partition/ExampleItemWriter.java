package org.springframework.batch.integration.partition;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;

/**
 * Dummy {@link ItemWriter} which only logs data it receives.
 */
public class ExampleItemWriter implements ItemWriter<Object> {

	private static final Log log = LogFactory.getLog(ExampleItemWriter.class);

	/**
	 * @see ItemWriter#write(List)
	 */
	public void write(List<? extends Object> data) throws Exception {
		log.info(data);
	}

}
