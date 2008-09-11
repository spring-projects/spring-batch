package org.springframework.batch.item.file;

import java.io.Writer;
import java.io.IOException;

/**
 * Callback interface for writing to a file - useful e.g. for handling headers
 * and footers.
 * 
 * @author Robert Kasanicky
 */
public interface FileWriterCallback {

	/**
	 * Write contents to a file using the supplied {@link Writer}. It is not
	 * required to flush the writer inside this method.
	 */
	void write(Writer writer) throws IOException;
}
