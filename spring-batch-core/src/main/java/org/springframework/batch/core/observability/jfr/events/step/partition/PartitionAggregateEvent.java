/*
 * Copyright 2025-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.observability.jfr.events.step.partition;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;

@Label("Partition Aggregate")
@Description("Partition Aggregate Event")
@Category({ "Spring Batch", "Step", "Partition" })
public class PartitionAggregateEvent extends Event {

	@Label("Step Name")
	public String stepName;

	@Label("Step Execution Id")
	public long stepExecutionId;

	public PartitionAggregateEvent(String stepName, long stepExecutionId) {
		this.stepName = stepName;
		this.stepExecutionId = stepExecutionId;
	}

}