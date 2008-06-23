package example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.AbstractItemWriter;
import org.springframework.batch.item.ItemWriter;


/**
 * Dummy {@link ItemWriter} which only logs data it receives.
 */
public class ExampleItemWriter extends AbstractItemWriter {

	private static final Log log = LogFactory.getLog(ExampleItemWriterTests.class);
	
	/**
	 * @see ItemWriter#write(Object)
	 */
	public void write(Object data) throws Exception {
		log.info(data);
	}

}
