package org.springframework.batch.io.driving;

import java.util.List;

import org.springframework.batch.item.ExecutionAttributes;

/**
 * Strategy interface used to generate keys in driving query input.
 * 
 * @author Lucas Ward
 * @since 1.0
 */
public interface KeyGenerator {

	/**
	 * @return list of keys returned by the driving query
	 */
	List retrieveKeys();

	/**
	 * Restore the keys list based on provided restart data.
	 *
	 * @param streamContext, the restart data to restore the keys list from.
	 * @return a list of keys.
	 * @throws IllegalArgumentException is streamContext is null.
	 */
	List restoreKeys(ExecutionAttributes streamContext);
	
	/**
	 * Return the provided key as restart data.
	 * 
	 * @param key to be converted to restart data.
	 * @return StreamContext representation of the key.
	 * @throws IllegalArgumentException if key is null.
	 * @throws IllegalArgumentException if key is an incompatible type.
	 */
	ExecutionAttributes getKeyAsStreamContext(Object key);
}
