package example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.AbstractItemWriter;


/**
 * Dummy {@link ItemWriter} which only logs data it receives.
 */
public class ExampleItemWriter extends AbstractItemWriter<Object> {

	private static final Log log = LogFactory.getLog(ExampleItemWriter.class);
	
	/**
	 * @see ItemWriter#write(Object)
	 */
	public void write(Object data) throws Exception {
		log.info(data);
	}

}
