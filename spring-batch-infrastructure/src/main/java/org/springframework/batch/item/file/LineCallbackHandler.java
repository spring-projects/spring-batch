package org.springframework.batch.item.file;

/**
 * Callback interface for handling a line from file. Useful e.g. for header
 * processing.
 * 
 * @author Robert Kasanicky
 */
public interface LineCallbackHandler {

	void handleLine(String line);
}
