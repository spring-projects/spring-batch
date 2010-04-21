package org.springframework.batch.core.partition;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;

/**
 * Dummy {@link ItemWriter} which only logs data it receives.
 */
public class ExampleItemWriter implements ItemWriter<String> {

	private static final Log log = LogFactory.getLog(ExampleItemWriter.class);
	
	private static List<String> items = new ArrayList<String>();
	
	public static void clear() {
		items.clear();
	}

	public static List<String> getItems() {
		return items;
	}

	/**
	 * @see ItemWriter#write(List)
	 */
	public void write(List<? extends String> data) throws Exception {
		log.info(data);
		items.addAll(data);
	}

}
