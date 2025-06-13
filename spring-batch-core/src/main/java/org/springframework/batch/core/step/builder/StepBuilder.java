/*
 * Copyright 2006-2025 the original author or authors.
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

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Convenient entry point for building all kinds of steps. Use this as a factory for
 * fluent builders of any step.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.2
 */
public class StepBuilder extends StepBuilderHelper<StepBuilder> {

	/**
	 * Initialize a step builder for a step with the given job repository. The name of the
	 * step will be set to the bean name by default.
	 * @param jobRepository the job repository to which the step should report to.
	 * @since 6.0
	 */
	public StepBuilder(JobRepository jobRepository) {
		super(jobRepository);
	}

	/**
	 * Initialize a step builder for a step with the given name and job repository.
	 * @param name the name of the step
	 * @param jobRepository the job repository to which the step should report to.
	 * @since 5.0
	 */
	public StepBuilder(String name, JobRepository jobRepository) {
		super(name, jobRepository);
	}

	/**
	 * Build a step with a custom tasklet, not necessarily item processing.
	 * @param tasklet a tasklet
	 * @param transactionManager the transaction manager to use for the tasklet
	 * @return a {@link TaskletStepBuilder}
	 * @since 5.0
	 */
	public TaskletStepBuilder tasklet(Tasklet tasklet, PlatformTransactionManager transactionManager) {
		return new TaskletStepBuilder(this).tasklet(tasklet, transactionManager);
	}

	/**
	 * Build a step that processes items in chunks with the size provided. To extend the
	 * step to being fault tolerant, call the {@link SimpleStepBuilder#faultTolerant()}
	 * method on the builder. In most cases you will want to parameterize your call to
	 * this method, to preserve the type safety of your readers and writers, e.g.
	 *
	 * <pre>
	 * new StepBuilder(&quot;step1&quot;).&lt;Order, Ledger&gt; chunk(100, transactionManager).reader(new OrderReader()).writer(new LedgerWriter())
	 * // ... etc.
	 * </pre>
	 * @param chunkSize the chunk size (commit interval)
	 * @param transactionManager the transaction manager to use for the chunk-oriented
	 * tasklet
	 * @return a {@link SimpleStepBuilder}
	 * @param <I> the type of item to be processed as input
	 * @param <O> the type of item to be output
	 * @since 5.0
	 */
	public <I, O> SimpleStepBuilder<I, O> chunk(int chunkSize, PlatformTransactionManager transactionManager) {
		return new SimpleStepBuilder<I, O>(this).transactionManager(transactionManager).chunk(chunkSize);
	}

	/**
	 * Build a step that processes items in chunks with the completion policy provided. To
	 * extend the step to being fault tolerant, call the
	 * {@link SimpleStepBuilder#faultTolerant()} method on the builder. In most cases you
	 * will want to parameterize your call to this method, to preserve the type safety of
	 * your readers and writers, e.g.
	 *
	 * <pre>
	 * new StepBuilder(&quot;step1&quot;).&lt;Order, Ledger&gt; chunk(100, transactionManager).reader(new OrderReader()).writer(new LedgerWriter())
	 * // ... etc.
	 * </pre>
	 * @param completionPolicy the completion policy to use to control chunk processing
	 * @param transactionManager the transaction manager to use for the chunk-oriented
	 * tasklet
	 * @return a {@link SimpleStepBuilder}
	 * @param <I> the type of item to be processed as input
	 * @param <O> the type of item to be output
	 * @since 5.0
	 */
	public <I, O> SimpleStepBuilder<I, O> chunk(CompletionPolicy completionPolicy,
			PlatformTransactionManager transactionManager) {
		return new SimpleStepBuilder<I, O>(this).transactionManager(transactionManager).chunk(completionPolicy);
	}

	/**
	 * Create a partition step builder for a remote (or local) step.
	 * @param stepName the name of the remote or delegate step
	 * @param partitioner a partitioner to be used to construct new step executions
	 * @return a {@link PartitionStepBuilder}
	 */
	public PartitionStepBuilder partitioner(String stepName, Partitioner partitioner) {
		return new PartitionStepBuilder(this).partitioner(stepName, partitioner);
	}

	/**
	 * Create a partition step builder for a remote (or local) step.
	 * @param step the step to execute in parallel
	 * @return a PartitionStepBuilder
	 */
	public PartitionStepBuilder partitioner(Step step) {
		return new PartitionStepBuilder(this).step(step);
	}

	/**
	 * Create a new step builder that will execute a job.
	 * @param job a job to execute
	 * @return a {@link JobStepBuilder}
	 */
	public JobStepBuilder job(Job job) {
		return new JobStepBuilder(this).job(job);
	}

	/**
	 * Create a new step builder that will execute a flow.
	 * @param flow a flow to execute
	 * @return a {@link FlowStepBuilder}
	 */
	public FlowStepBuilder flow(Flow flow) {
		return new FlowStepBuilder(this).flow(flow);
	}

	@Override
	protected StepBuilder self() {
		return this;
	}

}
