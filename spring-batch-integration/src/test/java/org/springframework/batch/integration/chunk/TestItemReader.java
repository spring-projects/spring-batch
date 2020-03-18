package org.springframework.batch.integration.chunk;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class TestItemReader<T> implements ItemReader<T> {

	private static final Log logger = LogFactory.getLog(TestItemReader.class);

	/**
	 * Counts the number of chunks processed in the handler.
	 */
	public volatile int count = 0;

	/**
	 * Item that causes failure in handler.
	 */
	public final static String FAIL_ON = "bad";

	/**
	 * Item that causes handler to wait to simulate delayed processing.
	 */
	public static final String WAIT_ON = "wait";

	private List<T> items = new ArrayList<>();
	
	/**
	 * @param items the items to set
	 */
	public void setItems(List<T> items) {
		this.items = items;
	}

	@Nullable
	public T read() throws Exception, UnexpectedInputException, ParseException {

		if (count>=items.size()) {
			return null;
		}

		T item = items.get(count++);

		logger.debug("Reading "+item);

		if (item.equals(WAIT_ON)) {
			try {
				Thread.sleep(200);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("Unexpected interruption.", e);
			}
		}

		if (item.equals(FAIL_ON)) {
			throw new IllegalStateException("Planned failure on: " + FAIL_ON);
		}
		
		return item;

	}

}
