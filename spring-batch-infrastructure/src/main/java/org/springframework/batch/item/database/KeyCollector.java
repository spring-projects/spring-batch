package org.springframework.batch.item.database;

import java.util.List;

import org.springframework.batch.item.ExecutionContext;

/**
 * Strategy interface used to collect keys in driving query input.
 * 
 * @author Lucas Ward
 * @see DrivingQueryItemReader
 */
public interface KeyCollector {

	/**
	 * <p>Retrieve the keys to be iterated over.  If the ExecutionContext
	 * provided includes any state relevant to this collector (because it was
	 * stored as part of saveState()) then it should start from the point
	 * indicated by that state.</p>
	 * 
	 * <p>In the case of a restart, (i.e. the ExecutionContext contains relevant state)
	 * this method should return only the keys that are remaining to be processed.  For
	 * example, if the are 1,000 keys, and the 500th is processed before the batch job
	 * terminates unexpectedly, upon restart keys 501 through 1,000 should be returned.
	 * </p>
	 * 
	 * @param executionContext ExecutionContext containing any potential initial state
	 * that could potentially be used to retrieve the correct keys.
	 * @return list of keys returned by the driving query
	 */
	List retrieveKeys(ExecutionContext executionContext);
	
	/**
	 * Given the provided key, store it in the provided ExecutionContext.  This
	 * is necessary because retrieveKeys() will be called with the ExecutionContext
	 * that is provided as a result of this method.  Since only the KeyCollector 
	 * can know what format the ExecutionContext should be in, it should also
	 * save the state, as opposed to the {@link DrivingQueryItemReader} doing it
	 * for all KeyCollector implementations.
	 * 
	 * @param key to be converted to restart data.
	 * @return {@link ExecutionContext} representation of the key.
	 * @throws IllegalArgumentException if key is null.
	 * @throws IllegalArgumentException if key is an incompatible type.
	 */
	void saveState(Object key, ExecutionContext executionContext);
}
