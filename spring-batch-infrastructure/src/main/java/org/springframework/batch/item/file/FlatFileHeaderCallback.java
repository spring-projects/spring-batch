package org.springframework.batch.item.file;

import java.io.Writer;
import java.io.IOException;

/**
 * Callback interface for writing to a header to a file.
 * 
 * @author Robert Kasanicky
 */
public interface FlatFileHeaderCallback {

	/**
	 * Write contents to a file using the supplied {@link Writer}. It is not
	 * required to flush the writer inside this method.
	 */
	void writeHeader(Writer writer) throws IOException;
}
