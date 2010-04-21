/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.batch.core.partition;

import java.util.Map;

import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.item.ExecutionContext;

/**
 * @author Dave Syer
 *
 */
public class MinMaxPartitioner extends SimplePartitioner {

	public Map<String, ExecutionContext> partition(int gridSize) {
		Map<String, ExecutionContext> partition = super.partition(gridSize);
		int total = 8; // The number of items in the ExampleItemReader
		int range = total/gridSize;
		int i = 0;
		for (ExecutionContext context : partition.values()) {
			 int min = (i++)*range;
			 int max = Math.min(total, (min+1)*range);
			 context.putInt("min", min);
			 context.putInt("max", max);
		}
		return partition;
	}

}
