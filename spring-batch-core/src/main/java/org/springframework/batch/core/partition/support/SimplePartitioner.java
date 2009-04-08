/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
 * @since 2.0
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
