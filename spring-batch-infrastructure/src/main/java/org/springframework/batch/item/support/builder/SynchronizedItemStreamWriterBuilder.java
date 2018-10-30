package org.springframework.batch.item.support.builder;

import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.support.SynchronizedItemStreamWriter;
import org.springframework.util.Assert;

/**
 * Creates a fully qualified {@link SynchronizedItemStreamWriter}.
 *
 * @author Dimitrios Liapis
 */
public class SynchronizedItemStreamWriterBuilder<T> {

	private ItemStreamWriter<T> delegate;

	public SynchronizedItemStreamWriterBuilder<T> delegate(ItemStreamWriter<T> delegate) {
		this.delegate = delegate;

		return this;
	}

	public SynchronizedItemStreamWriter<T> build() {
		Assert.notNull(this.delegate, "A delegate is required");

		SynchronizedItemStreamWriter<T> writer = new SynchronizedItemStreamWriter<>();
		writer.setDelegate(this.delegate);
		return writer;
	}
}
