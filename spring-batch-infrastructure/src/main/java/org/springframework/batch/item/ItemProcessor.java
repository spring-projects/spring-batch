package org.springframework.batch.item;

/**
 * <p>Interface for item transformation.  Given an item as input, this interface provides
 * an extension point which allows for the application of business logic in an item 
 * oriented processing scenario.  It should be noted that while it's possible to return
 * a different type than the one provided, it's not strictly necessary.  Furthermore, 
 * returning null indicates that the item should not be continued to be processed.</p>
 *  
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public interface ItemProcessor<I, O> {

	/**
	 * Process the provided item, returning a potentially modified or new item for continued
	 * processing.  If the returned result is null, it is assumed that processing of the item
	 * should not continue.
	 * 
	 * @param item to be processed
	 * @return potentially modified or new item for continued processing, null if processing of the 
	 *  provided item should not continue.
	 * @throws Exception
	 */
	O process(I item) throws Exception;
}
