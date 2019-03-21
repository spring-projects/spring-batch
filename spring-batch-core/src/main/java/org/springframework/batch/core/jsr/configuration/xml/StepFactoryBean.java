/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.xml;

import javax.batch.api.Batchlet;
import javax.batch.api.chunk.CheckpointAlgorithm;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.api.chunk.ItemReader;
import javax.batch.api.chunk.ItemWriter;
import javax.batch.api.partition.PartitionReducer;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.xml.StepParserStepFactoryBean;
import org.springframework.batch.core.jsr.configuration.support.BatchPropertyContext;
import org.springframework.batch.core.jsr.partition.JsrPartitionHandler;
import org.springframework.batch.core.jsr.step.batchlet.BatchletAdapter;
import org.springframework.batch.core.jsr.step.builder.JsrBatchletStepBuilder;
import org.springframework.batch.core.jsr.step.builder.JsrFaultTolerantStepBuilder;
import org.springframework.batch.core.jsr.step.builder.JsrPartitionStepBuilder;
import org.springframework.batch.core.jsr.step.builder.JsrSimpleStepBuilder;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.builder.TaskletStepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.jsr.item.ItemProcessorAdapter;
import org.springframework.batch.jsr.item.ItemReaderAdapter;
import org.springframework.batch.jsr.item.ItemWriterAdapter;
import org.springframework.batch.jsr.repeat.CheckpointAlgorithmAdapter;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.policy.CompositeCompletionPolicy;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.policy.TimeoutTerminationPolicy;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;

/**
 * This {@link FactoryBean} is used by the JSR-352 namespace parser to create
 * {@link Step} objects. It stores all of the properties that are
 * configurable on the &lt;step/&gt;.
 *
 * @author Michael Minella
 * @author Chris Schaefer
 * @since 3.0
 */
public class StepFactoryBean<I, O> extends StepParserStepFactoryBean<I, O> {

	@SuppressWarnings("unused")
	private int partitions;
	private BatchPropertyContext batchPropertyContext;

	private PartitionReducer reducer;

	private Integer timeout;

	public void setPartitionReducer(PartitionReducer reducer) {
		this.reducer = reducer;
	}

	public void setBatchPropertyContext(BatchPropertyContext context) {
		this.batchPropertyContext = context;
	}

	public void setPartitions(int partitions) {
		this.partitions = partitions;
	}

	/**
	 * Create a {@link Step} from the configuration provided.
	 *
	 * @see FactoryBean#getObject()
	 */
	@Override
	public Step getObject() throws Exception {
		if(hasPartitionElement()) {
			return createPartitionStep();
		}
		else if (hasChunkElement()) {
			Assert.isTrue(!hasTasklet(), "Step [" + getName()
					+ "] has both a <chunk/> element and a 'ref' attribute  referencing a Tasklet.");

			validateFaultTolerantSettings();

			if (isFaultTolerant()) {
				return createFaultTolerantStep();
			}
			else {
				return createSimpleStep();
			}
		}
		else if (hasTasklet()) {
			return createTaskletStep();
		}
		else {
			return createFlowStep();
		}
	}

	/**
	 * @return a new {@link TaskletStep}
	 */
	@Override
	protected TaskletStep createTaskletStep() {
		JsrBatchletStepBuilder jsrBatchletStepBuilder = new JsrBatchletStepBuilder(new StepBuilder(getName()));
		jsrBatchletStepBuilder.setBatchPropertyContext(batchPropertyContext);
		TaskletStepBuilder builder = jsrBatchletStepBuilder.tasklet(getTasklet());
		enhanceTaskletStepBuilder(builder);
		return builder.build();
	}

	@Override
	protected void setChunk(SimpleStepBuilder<I, O> builder) {
		if(timeout != null && getCommitInterval() != null) {
			CompositeCompletionPolicy completionPolicy = new CompositeCompletionPolicy();
			CompletionPolicy [] policies = new CompletionPolicy[2];
			policies[0] = new SimpleCompletionPolicy(getCommitInterval());
			policies[1] = new TimeoutTerminationPolicy(timeout * 1000);
			completionPolicy.setPolicies(policies);
			builder.chunk(completionPolicy);
		} else if(timeout != null) {
			builder.chunk(new TimeoutTerminationPolicy(timeout * 1000));
		} else if(getCommitInterval() != null) {
			builder.chunk(getCommitInterval());
		}

		if(getCompletionPolicy() != null) {
			builder.chunk(getCompletionPolicy());
		}
	}


	@Override
	protected Step createPartitionStep() {
		// Creating a partitioned step for the JSR needs to create two steps...the partitioned step and the step being executed.
		Step executedStep = null;

		if (hasChunkElement()) {
			Assert.isTrue(!hasTasklet(), "Step [" + getName()
					+ "] has both a <chunk/> element and a 'ref' attribute  referencing a Tasklet.");

			validateFaultTolerantSettings();

			if (isFaultTolerant()) {
				executedStep = createFaultTolerantStep();
			}
			else {
				executedStep = createSimpleStep();
			}
		}
		else if (hasTasklet()) {
			executedStep = createTaskletStep();
		}

		((JsrPartitionHandler) super.getPartitionHandler()).setStep(executedStep);

		JsrPartitionStepBuilder builder = new JsrSimpleStepBuilder<I, O>(new StepBuilder(executedStep.getName())).partitioner(executedStep);

		enhanceCommonStep(builder);

		if (getPartitionHandler() != null) {
			builder.partitionHandler(getPartitionHandler());
		}

		if(reducer != null) {
			builder.reducer(reducer);
		}

		builder.aggregator(getStepExecutionAggergator());

		return builder.build();
	}

	/**
	 * Wraps a {@link Batchlet} in a {@link BatchletAdapter} if required for consumption
	 * by the rest of the framework.
	 *
	 * @param tasklet {@link Tasklet} or {@link Batchlet} implementation
	 * @throws IllegalArgumentException if tasklet does not implement either Tasklet or Batchlet
	 */
	public void setStepTasklet(Object tasklet) {
		if(tasklet instanceof Tasklet) {
			super.setTasklet((Tasklet) tasklet);
		} else if(tasklet instanceof Batchlet){
			super.setTasklet(new BatchletAdapter((Batchlet) tasklet));
		} else {
			throw new IllegalArgumentException("The field tasklet must reference an implementation of " +
					"either org.springframework.batch.core.step.tasklet.Tasklet or javax.batch.api.Batchlet");
		}
	}

	/**
	 * Wraps a {@link ItemReader} in a {@link ItemReaderAdapter} if required for consumption
	 * by the rest of the framework.
	 *
	 * @param itemReader {@link ItemReader} or {@link org.springframework.batch.item.ItemReader} implementation
	 * @throws IllegalArgumentException if itemReader does not implement either version of ItemReader
	 */
	@SuppressWarnings("unchecked")
	public void setStepItemReader(Object itemReader) {
		if(itemReader instanceof org.springframework.batch.item.ItemReader) {
			super.setItemReader((org.springframework.batch.item.ItemReader<I>) itemReader);
		} else if(itemReader instanceof ItemReader){
			super.setItemReader(new ItemReaderAdapter<>((ItemReader) itemReader));
		} else {
			throw new IllegalArgumentException("The definition of an item reader must implement either " +
					"org.springframework.batch.item.ItemReader or javax.batch.api.chunk.ItemReader");
		}
	}

	/**
	 * Wraps a {@link ItemProcessor} in a {@link ItemProcessorAdapter} if required for consumption
	 * by the rest of the framework.
	 *
	 * @param itemProcessor {@link ItemProcessor} or {@link org.springframework.batch.item.ItemProcessor} implementation
	 * @throws IllegalArgumentException if itemProcessor does not implement either version of ItemProcessor
	 */
	@SuppressWarnings("unchecked")
	public void setStepItemProcessor(Object itemProcessor) {
		if(itemProcessor instanceof org.springframework.batch.item.ItemProcessor) {
			super.setItemProcessor((org.springframework.batch.item.ItemProcessor<I, O>) itemProcessor);
		} else if(itemProcessor instanceof ItemProcessor){
			super.setItemProcessor(new ItemProcessorAdapter<>((ItemProcessor) itemProcessor));
		} else {
			throw new IllegalArgumentException("The definition of an item processor must implement either " +
					"org.springframework.batch.item.ItemProcessor or javax.batch.api.chunk.ItemProcessor");
		}
	}

	/**
	 * Wraps a {@link ItemWriter} in a {@link ItemWriterAdapter} if required for consumption
	 * by the rest of the framework.
	 *
	 * @param itemWriter {@link ItemWriter} or {@link org.springframework.batch.item.ItemWriter} implementation
	 * @throws IllegalArgumentException if itemWriter does not implement either version of ItemWriter
	 */
	@SuppressWarnings("unchecked")
	public void setStepItemWriter(Object itemWriter) {
		if(itemWriter instanceof org.springframework.batch.item.ItemWriter) {
			super.setItemWriter((org.springframework.batch.item.ItemWriter<O>) itemWriter);
		} else if(itemWriter instanceof ItemWriter){
			super.setItemWriter(new ItemWriterAdapter<>((ItemWriter) itemWriter));
		} else {
			throw new IllegalArgumentException("The definition of an item writer must implement either " +
					"org.springframework.batch.item.ItemWriter or javax.batch.api.chunk.ItemWriter");
		}
	}

	/**
	 * Wraps a {@link CheckpointAlgorithm} in a {@link CheckpointAlgorithmAdapter} if required for consumption
	 * by the rest of the framework.
	 *
	 * @param chunkCompletionPolicy {@link CompletionPolicy} or {@link CheckpointAlgorithm} implementation
	 * @throws IllegalArgumentException if chunkCompletionPolicy does not implement either CompletionPolicy or CheckpointAlgorithm
	 */
	public void setStepChunkCompletionPolicy(Object chunkCompletionPolicy) {
		if(chunkCompletionPolicy instanceof CompletionPolicy) {
			super.setChunkCompletionPolicy((CompletionPolicy) chunkCompletionPolicy);
		} else if(chunkCompletionPolicy instanceof CheckpointAlgorithm) {
			super.setChunkCompletionPolicy(new CheckpointAlgorithmAdapter((CheckpointAlgorithm) chunkCompletionPolicy));
		} else {
			throw new IllegalArgumentException("The definition of a chunk completion policy must implement either " +
					"org.springframework.batch.repeat.CompletionPolicy or javax.batch.api.chunk.CheckpointAlgorithm");
		}
	}

	@Override
	protected FaultTolerantStepBuilder<I, O> getFaultTolerantStepBuilder(String stepName) {
		JsrFaultTolerantStepBuilder<I, O> jsrFaultTolerantStepBuilder = new JsrFaultTolerantStepBuilder<>(
                new StepBuilder(stepName));
		jsrFaultTolerantStepBuilder.setBatchPropertyContext(batchPropertyContext);
		return jsrFaultTolerantStepBuilder;
	}

	@Override
	protected SimpleStepBuilder<I, O> getSimpleStepBuilder(String stepName) {
		JsrSimpleStepBuilder<I, O> jsrSimpleStepBuilder = new JsrSimpleStepBuilder<>(new StepBuilder(stepName));
		jsrSimpleStepBuilder.setBatchPropertyContext(batchPropertyContext);
		return jsrSimpleStepBuilder;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}
}
