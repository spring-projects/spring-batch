package org.springframework.batch.sample.item.reader;

import java.math.BigDecimal;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.exception.MarkFailedException;
import org.springframework.batch.item.exception.ResetFailedException;
import org.springframework.batch.item.exception.StreamException;
import org.springframework.batch.item.reader.AbstractItemReaderRecoverer;
import org.springframework.batch.sample.domain.Trade;

/**
 * Generates configurable number of {@link Trade} items.
 * 
 * @author Robert Kasanicky
 */
public class GeneratingItemReader extends AbstractItemReaderRecoverer implements ItemStream {

	private int limit = 1;
	
	private int counter = 0;

	private int marked;
	
	public Object read() throws Exception {
		if (counter < limit) {
			counter++;
			return new Trade(
					"isin" + counter, 
					counter, 
					new BigDecimal(counter), 
					"customer" + counter);
		}
		return null;
	}

	/**
	 * @param limit number of items that will be generated
	 * (null returned on consecutive calls).
	 */
	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getCounter() {
		return counter;
	}

	public int getLimit() {
		return limit;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemRecoverer#recover(java.lang.Object, java.lang.Throwable)
	 */
	public boolean recover(Object data, Throwable cause) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#close()
	 */
	public void close() throws StreamException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#mark()
	 */
	public void mark() throws MarkFailedException {
		this.marked = this.counter;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#reset()
	 */
	public void reset() throws ResetFailedException {
		this.counter = this.marked;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#restoreFrom(org.springframework.batch.item.ExecutionContext)
	 */
	public void restoreFrom(ExecutionContext context) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ExecutionContextProvider#getExecutionContext()
	 */
	public void update() {
	}

	public void open(ExecutionContext context) throws StreamException {
	}

}
