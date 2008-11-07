package org.springframework.batch.item.file;

import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.core.io.Resource;

/**
 * Interface for {@link ItemWriter}s that implement {@link ItemStream} and write
 * output to {@link Resource}.
 * 
 * @author Robert Kasanicky
 */
public interface ResourceAwareItemWriterItemStream<T> extends ItemStream, ItemWriter<T> {

	void setResource(Resource resource);
}
