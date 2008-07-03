package example;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.support.AbstractItemReader;

/**
 * {@link ItemReader} with hard-coded input data.
 */
public class ExampleItemReader extends AbstractItemReader {
	
	private String[] input = {"Hello world!", null};
	
	private int index = 0;
	
	/**
	 * Reads next record from input
	 */
	public Object read() throws Exception {
		return input[index++];
	}

}
