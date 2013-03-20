package org.springframework.batch.item.file;


/**
 * Tests for {@link MultiThreadedFlatFileItemReader}.
 */
public class MultiThreadedFlatFileItemReaderTests extends FlatFileItemReaderTests {

	private MultiThreadedFlatFileItemReader<String> reader = new MultiThreadedFlatFileItemReader<String>();

	private MultiThreadedFlatFileItemReader<Item> itemReader = new MultiThreadedFlatFileItemReader<Item>();
	
	@Override
	protected FlatFileItemReader<String> getReader() {
		return reader;
	}

	@Override
	protected FlatFileItemReader<Item> getItemReader() {
		return itemReader;
	}

}
