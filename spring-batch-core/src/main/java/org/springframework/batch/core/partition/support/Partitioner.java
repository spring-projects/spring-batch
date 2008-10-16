package org.springframework.batch.core.partition.support;

import java.util.Map;

import org.springframework.batch.item.ExecutionContext;

/**
 * Central strategy interface for creating input parameters for a partitioned
 * step in the form of {@link ExecutionContext} instances. The usual aim is to
 * create a set of distinct input values, e.g. a set of non-overlapping primary
 * key ranges, or a set of unique filenames.
 * 
 * @author Dave Syer
 * 
 */
public interface Partitioner {

	/**
	 * Create a set of distinct {@link ExecutionContext} instances together with
	 * a unique identifier for each one. The identifiers should be short,
	 * mnemonic values, and only have to be unique within the return value (e.g.
	 * use an incrementer).
	 * 
	 * @param gridSize the size of the map to return
	 * @return a map from identifier to input parameters
	 */
	Map<String, ExecutionContext> partition(int gridSize);

}
