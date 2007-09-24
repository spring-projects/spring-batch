package org.springframework.batch.io.stax;

import javax.xml.stream.XMLEventReader;


/**
 * Interface for event readers which support treating XML fragments as standalone XML documents
 * by wrapping the fragments with StartDocument and EndDocument events.
 * 
 * @author Robert Kasanicky
 */
interface FragmentEventReader extends XMLEventReader {

	/**
	 * Tells the event reader its cursor position is exactly before the fragment.
	 */
	void markStartFragment();
	
	/**
	 * Tells the event reader the current fragment has been processed.
	 * If the cursor is still inside the fragment it should be moved
	 * after the end of the fragment.
	 */
	void markFragmentProcessed();
	
	/**
	 * Reset the state of the fragment reader - make it forget
	 * it assumptions about current position of cursor
	 * (e.g. in case of rollback of the wrapped reader).
	 */
	void reset();

}
