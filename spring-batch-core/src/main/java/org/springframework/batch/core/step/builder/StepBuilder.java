/*
 * Copyright 2006-2013 the original author or authors.
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
package org.springframework.batch.core.step.builder;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.CompletionPolicy;

/**
 * Convenient entry point for building all kinds of steps. Use this as a factory for fluent builders of any step.
 *
 * @author Dave Syer
 *
 * @since 2.2
 */
public class StepBuilder extends StepBuilderHelper<StepBuilder> {

	/**
	 * Initialize a step builder for a step with the given name.
	 *
	 * @param name the name of the step
	 */
	public StepBuilder(String name) {
		super(name);
	}

	/**
	 * Build a step with a custom tasklet, not necessarily item processing.
	 *
	 * @param tasklet a tasklet
	 * @return a {@link TaskletStepBuilder}
	 */
	public TaskletStepBuilder tasklet(Tasklet tasklet) {
		return new TaskletStepBuilder(this).tasklet(tasklet);
	}

	/**
	 * Build a step that processes items in chunks with the size provided. To extend the step to being fault tolerant,
	 * call the {@link SimpleStepBuilder#faultTolerant()} method on the builder. In most cases you will want to
	 * parameterize your call to this method, to preserve the type safety of your readers and writers, e.g.
	 *
	 * <pre>
	 * new StepBuilder(&quot;step1&quot;).&lt;Order, Ledger&gt; chunk(100).reader(new OrderReader()).writer(new LedgerWriter())
	 * // ... etc.
	 * </pre>
	 *
	 * @param chunkSize the chunk size (commit interval)
	 * @return a {@link SimpleStepBuilder}
	 * @param <I> the type of item to be processed as input
	 * @param <O> the type of item to be output
	 */
	public <I, O> SimpleStepBuilder<I, O> chunk(int chunkSize) {
		return new SimpleStepBuilder<I, O>(this).chunk(chunkSize);
	}

	/**
	 * Build a step that processes items in chunks with the completion policy provided. To extend the step to being
	 * fault tolerant, call the {@link SimpleStepBuilder#faultTolerant()} method on the builder. In most cases you will
	 * want to parameterize your call to this method, to preserve the type safety of your readers and writers, e.g.
	 *
	 * <pre>
	 * new StepBuilder(&quot;step1&quot;).&lt;Order, Ledger&gt; chunk(100).reader(new OrderReader()).writer(new LedgerWriter())
	 * // ... etc.
	 * </pre>
	 *
	 * @param completionPolicy the completion policy to use to control chunk processing
	 * @return a {@link SimpleStepBuilder}
	 * @param <I> the type of item to be processed as input
	 * @param <O> the type of item to be output *
	 */
	public <I, O> SimpleStepBuilder<I, O> chunk(CompletionPolicy completionPolicy) {
		return new SimpleStepBuilder<I, O>(this).chunk(completionPolicy);
	}

	/**
	 * Create a partition step builder for a remote (or local) step.
	 *
	 * @param stepName the name of the remote or delegate step
	 * @param partitioner a partitioner to be used to construct new step executions
	 * @return a {@link PartitionStepBuilder}
	 */
	public PartitionStepBuilder partitioner(String stepName, Partitioner partitioner) {
		return new PartitionStepBuilder(this).partitioner(stepName, partitioner);
	}

	/**
	 * Create a partition step builder for a remote (or local) step.
	 *
	 * @param step the step to execute in parallel
	 * @return a PartitionStepBuilder
	 */
	public PartitionStepBuilder partitioner(Step step) {
		return new PartitionStepBuilder(this).step(step);
	}

	/**
	 * Create a new step builder that will execute a job.
	 *
	 * @param job a job to execute
	 * @return a {@link JobStepBuilder}
	 */
	public JobStepBuilder job(Job job) {
		return new JobStepBuilder(this).job(job);
	}

	/**
	 * Create a new step builder that will execute a flow.
	 *
	 * @param flow a flow to execute
	 * @return a {@link FlowStepBuilder}
	 */
	public FlowStepBuilder flow(Flow flow) {
		return new FlowStepBuilder(this).flow(flow);
	}

}
