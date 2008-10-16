package org.springframework.batch.core.partition.support;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.item.ExecutionContext;

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
