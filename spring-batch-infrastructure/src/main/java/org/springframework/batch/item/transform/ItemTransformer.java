package org.springframework.batch.item.transform;

/**
 * Interface for item transformations during processing phase.
 * 
 * @author Robert Kasanicky
 */
public interface ItemTransformer {

	Object transform(Object item) throws Exception;
}
