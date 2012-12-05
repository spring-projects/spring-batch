/*
 * Copyright 2006-2011 the original author or authors.
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
package org.springframework.batch.core.step.builder;


import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.CompletionPolicy;

/**
 * @author Dave Syer
 * 
 */
public class StepBuilder extends StepBuilderHelper<StepBuilder> {

	public StepBuilder(String name) {
		super(name);
	}

	public TaskletStepBuilder tasklet(Tasklet tasklet) {
		return new TaskletStepBuilder(this).tasklet(tasklet);
	}

	public <I, O> SimpleStepBuilder<I, O> chunk(int chunkSize) {
		return new SimpleStepBuilder<I, O>(this).chunk(chunkSize);
	}

	public <I, O> SimpleStepBuilder<I, O> chunk(CompletionPolicy completionPolicy) {
		return new SimpleStepBuilder<I, O>(this).completionPolicy(completionPolicy);
	}

	public PartitionStepBuilder partitioner(String stepName, Partitioner partitioner) {
		return new PartitionStepBuilder(this).partitioner(stepName, partitioner);
	}

	public PartitionStepBuilder partitioner(Step step) {
		return new PartitionStepBuilder(this).step(step);
	}

	public JobStepBuilder job(Job job) {
		return new JobStepBuilder(this).job(job);
	}

	public FlowStepBuilder flow(Flow flow) {
		return new FlowStepBuilder(this).flow(flow);
	}

}
