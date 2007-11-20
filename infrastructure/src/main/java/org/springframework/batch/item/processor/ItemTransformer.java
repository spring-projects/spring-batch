package org.springframework.batch.item.processor;

/**
 * Interface for item transformations during processing phase.
 * 
 * @author Robert Kasanicky
 */
public interface ItemTransformer {

	Object transform(Object item);
}
