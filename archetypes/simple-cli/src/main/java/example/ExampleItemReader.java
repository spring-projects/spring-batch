package example;

import org.springframework.batch.item.ItemReader;

/**
 * {@link ItemReader} with hard-coded input data.
 */
public class ExampleItemReader implements ItemReader<String> {
	
	private String[] input = {"Hello world!", null};
	
	private int index = 0;
	
	/**
	 * Reads next record from input
	 */
	public String read() throws Exception {
		return input[index++];
	}

}
