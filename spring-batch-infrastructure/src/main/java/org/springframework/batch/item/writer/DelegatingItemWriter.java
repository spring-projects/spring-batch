package org.springframework.batch.item.writer;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.exception.StreamException;
import org.springframework.batch.item.stream.ItemStreamAdapter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Simple wrapper around {@link ItemWriter}.
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 */
public class DelegatingItemWriter implements ItemWriter, ItemStream, InitializingBean {

	private ItemWriter writer;
	
	private ItemStream stream;

	/**
	 * Calls {@link #doProcess(Object)} and then writes the result to the
	 * delegate {@link ItemWriter}.
	 * @throws Exception 
	 * 
	 * @see ItemWriter#process(java.lang.Object)
	 */
	final public void write(Object item) throws Exception {
		Object result = doProcess(item);
		writer.write(result);
	}

	/**
	 * By default returns the argument. This method is an extension point meant
	 * to be overridden by subclasses that implement processing logic.
	 * @throws Exception 
	 */
	protected Object doProcess(Object item) throws Exception {
		return item;
	}

	/**
	 * Setter for {@link ItemWriter}.
	 */
	public void setDelegate(ItemWriter writer) {
		this.writer = writer;
		if (writer instanceof ItemStream) {
			this.stream = (ItemStream) writer;
		} else {
			this.stream = new ItemStreamAdapter();
		}
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(writer);
	}

	/**
	 * Delegates to {@link ItemWriter#clear()}
	 */
	public void clear() throws Exception {
		writer.clear();
	}

	/**
	 * Delegates to {@link ItemWriter#flush()}
	 */
	public void flush() throws Exception {
		writer.flush();
	}

	/**
	 * @throws StreamException
	 * @see org.springframework.batch.item.ItemStream#close()
	 */
	public void close() throws StreamException {
		stream.close();
	}

	/**
	 * @return
	 * @see org.springframework.batch.item.ExecutionContextProvider#getExecutionContext()
	 */
	public ExecutionContext getExecutionContext() {
		return stream.getExecutionContext();
	}

	/**
	 * @throws StreamException
	 * @see org.springframework.batch.item.ItemStream#open()
	 */
	public void open() throws StreamException {
		stream.open();
	}

	/**
	 * @param context
	 * @see org.springframework.batch.item.ItemStream#restoreFrom(org.springframework.batch.item.ExecutionContext)
	 */
	public void restoreFrom(ExecutionContext context) {
		stream.restoreFrom(context);
	}
	
	

}
