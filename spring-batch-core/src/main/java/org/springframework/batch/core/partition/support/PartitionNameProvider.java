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

package org.springframework.batch.core.partition.support;

import java.util.Collection;

/**
 * <p>
 * Optional interface for {@link Partitioner} implementations that need to use a
 * custom naming scheme for partitions. It is not necessary to implement this
 * interface if a partitioner extends {@link SimplePartitioner} and re-uses the
 * default partition names.
 * </p>
 * <p>
 * If a partitioner does implement this interface, however, on a restart the
 * {@link Partitioner#partition(int)} method will not be called again, instead
 * the partitions will be re-used from the last execution, and matched by name
 * with the results of {@link PartitionNameProvider#getPartitionNames(int)}.
 * This can be a useful performance optimisation if the partitioning process is
 * expensive.
 * </p>
 * 
 * @author Dave Syer
 * 
 * @since 2.1.3
 * 
 */
public interface PartitionNameProvider {

	Collection<String> getPartitionNames(int gridSize);

}
