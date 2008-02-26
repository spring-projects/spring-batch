package org.springframework.batch.io.driving;

import java.util.List;

import org.springframework.batch.item.ExecutionContext;

/**
 * Strategy interface used to generate keys in driving query input.
 * 
 * @author Lucas Ward
 * @since 1.0
 */
public interface KeyGenerator {

	/**
	 * @param executionContext TODO
	 * @return list of keys returned by the driving query
	 */
	List retrieveKeys(ExecutionContext executionContext);
	
	/**
	 * Return the provided key as restart data.
	 * 
	 * @param key to be converted to restart data.
	 * @return {@link ExecutionContext} representation of the key.
	 * @throws IllegalArgumentException if key is null.
	 * @throws IllegalArgumentException if key is an incompatible type.
	 */
	void saveState(Object key, ExecutionContext executionContext);
}
