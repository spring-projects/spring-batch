package org.springframework.batch.item.transform;

/**
 * Interface for item transformations during processing phase.
 * 
 * @author Robert Kasanicky
 */
public interface ItemTransformer<I,O> {

	O transform(I item) throws Exception;
}
