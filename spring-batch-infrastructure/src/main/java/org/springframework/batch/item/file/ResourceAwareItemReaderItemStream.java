package org.springframework.batch.item.file;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.core.io.Resource;

/**
 * Interface for {@link ItemReader}s that implement {@link ItemStream} and read
 * input from {@link Resource}.
 * 
 * @author Robert Kasanicky
 */
public interface ResourceAwareItemReaderItemStream extends ItemReader, ItemStream {

	void setResource(Resource resource);
}
