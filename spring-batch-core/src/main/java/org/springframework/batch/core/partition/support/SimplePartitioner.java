package org.springframework.batch.core.partition.support;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.item.ExecutionContext;

/**
 * Simplest possible implementation of {@link Partitioner}. Just creates a set
 * of empty {@link ExecutionContext} instances, and labels them as
 * <code>{partition0, partition1, ..., partitionN}</code>, where <code>N</code> is the grid
 * size.
 * 
 * @author Dave Syer
 * 
 */
public class SimplePartitioner implements Partitioner {

	private static final String PARTITION_KEY = "partition";

	public Map<String, ExecutionContext> partition(int gridSize) {
		Map<String, ExecutionContext> map = new HashMap<String, ExecutionContext>(gridSize);
		for (int i = 0; i < gridSize; i++) {
			map.put(PARTITION_KEY + i, new ExecutionContext());
		}
		return map;
	}

}
