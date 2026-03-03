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
package org.springframework.batch.core.observability.jfr.events.step.tasklet;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import org.jspecify.annotations.Nullable;

@Label("Tasklet Execution")
@Description("Tasklet Execution Event")
@Category({ "Spring Batch", "Step", "Tasklet" })
public class TaskletExecutionEvent extends Event {

	@Label("Step Name")
	public String stepName;

	@Label("Step Execution Id")
	public long stepExecutionId;

	@Label("Tasklet Type")
	public String taskletType;

	@Label("Tasklet Status")
	public @Nullable String taskletStatus;

	public TaskletExecutionEvent(String stepName, long stepExecutionId, String taskletType) {
		this.taskletType = taskletType;
		this.stepName = stepName;
		this.stepExecutionId = stepExecutionId;
	}

}