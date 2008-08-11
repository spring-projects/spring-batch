package org.springframework.batch.item;

/**
 * Interface for item transformations. If the return value is null it may be
 * ignored, so this interface is also able to act as a filter.
 * 
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public interface ItemProcessor<I, O> {

	O process(I item) throws Exception;
}
