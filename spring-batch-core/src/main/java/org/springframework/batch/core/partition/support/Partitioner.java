package org.springframework.batch.core.partition.support;

import java.util.Map;

import org.springframework.batch.item.ExecutionContext;

public interface Partitioner {

	Map<String, ExecutionContext> partition(int gridSize);
	
}
