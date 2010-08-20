/*
 * Copyright 2006-2010 the original author or authors.
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

package org.springframework.batch.integration.chunk;

import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.step.item.Chunk;
import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.batch.core.step.item.ChunkProcessor;
import org.springframework.batch.core.step.item.FaultTolerantChunkProcessor;
import org.springframework.batch.core.step.item.SimpleChunkProcessor;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Convenient factory bean for a chunk handler that also converts an existing chunk-oriented step into a remote chunk
 * master. The idea is to lift the existing chunk processor out of a Step that works locally, and replace it with a one
 * that writes chunks into a message channel. The existing step hands its business chunk processing responsibility over
 * to the handler produced by the factory, which then needs to be set up as a worker on the other end of the channel the
 * chunks are being sent to. Once this chunk handler is installed the application is playing the role of both the master
 * and the slave listeners in the Remote Chunking pattern for the Step in question.
 * 
 * @author Dave Syer
 * 
 */
public class RemoteChunkHandlerFactoryBean<T> implements FactoryBean<ChunkHandler<T>> {

	private static Log logger = LogFactory.getLog(RemoteChunkHandlerFactoryBean.class);

	private TaskletStep step;

	private ItemWriter<T> chunkWriter;

	private StepContributionSource stepContributionSource;

	/**
	 * The local step that is to be converted to a remote chunk master.
	 * 
	 * @param step the step to set
	 */
	public void setStep(TaskletStep step) {
		this.step = step;
	}

	/**
	 * The item writer to be injected into the step. Its responsibility is to send chunks of items to remote workers.
	 * Usually in practice it will be a {@link ChunkMessageChannelItemWriter}.
	 * 
	 * @param chunkWriter the chunk writer to set
	 */
	public void setChunkWriter(ItemWriter<T> chunkWriter) {
		this.chunkWriter = chunkWriter;
	}

	/**
	 * A source of {@link StepContribution} instances coming back from remote workers.
	 * 
	 * @param stepContributionSource the step contribution source to set (defaults to the chunk writer)
	 */
	public void setStepContributionSource(StepContributionSource stepContributionSource) {
		this.stepContributionSource = stepContributionSource;
	}

	/**
	 * The type of object created by this factory. Returns {@link ChunkHandler} class.
	 * 
	 * @see FactoryBean#getObjectType()
	 */
	public Class<?> getObjectType() {
		return ChunkHandler.class;
	}

	/**
	 * Optimization for the bean facctory (always returns true).
	 * 
	 * @see FactoryBean#isSingleton()
	 */
	public boolean isSingleton() {
		return true;
	}

	/**
	 * Builds a {@link ChunkHandler} from the {@link ChunkProcessor} extracted from the {@link #setStep(TaskletStep)
	 * step} provided. Also modifies the step to send chunks to the chunk handler via the
	 * {@link #setChunkWriter(ItemWriter) chunk writer}.
	 * 
	 * @see FactoryBean#getObject()
	 */
	public ChunkHandler<T> getObject() throws Exception {

		if (stepContributionSource == null) {
			Assert.state(chunkWriter instanceof StepContributionSource,
					"The chunk writer must be a StepContributionSource or else the source must be provided explicitly");
			stepContributionSource = (StepContributionSource) chunkWriter;
		}

		Assert.state(step instanceof TaskletStep, "Step [" + step.getName() + "] must be a TaskletStep");
		logger.debug("Converting TaskletStep with name=" + step.getName());

		Tasklet tasklet = getTasklet((TaskletStep) step);
		Assert.state(tasklet instanceof ChunkOrientedTasklet<?>, "Tasklet must be ChunkOrientedTasklet in step="
				+ step.getName());

		ChunkProcessor<T> chunkProcessor = getChunkProcessor((ChunkOrientedTasklet<?>) tasklet);
		Assert.state(chunkProcessor != null, "ChunkProcessor must be accessible in Tasklet in step=" + step.getName());

		ItemWriter<T> itemWriter = getItemWriter(chunkProcessor);
		Assert.state(!(itemWriter instanceof ChunkMessageChannelItemWriter<?>), "Cannot adapt step [" + step.getName()
				+ "] because it already has a remote chunk writer.  Use a local writer in the step.");

		replaceChunkProcessor((ChunkOrientedTasklet<?>) tasklet, chunkWriter, stepContributionSource);
		if (chunkWriter instanceof StepExecutionListener) {
			step.registerStepExecutionListener((StepExecutionListener) chunkWriter);
		}

		ChunkProcessorChunkHandler<T> handler = new ChunkProcessorChunkHandler<T>();
		setNonBuffering(chunkProcessor);
		handler.setChunkProcessor(chunkProcessor);
		// TODO: create step context for the processor in case it has
		// scope="step" dependencies
		handler.afterPropertiesSet();

		return handler;

	}

	/**
	 * Overrides the buffering settings in the chunk processor if it is fault tolerant.
	 * @param chunkProcessor the chunk processor that is going to be used in the workers
	 */
	private void setNonBuffering(ChunkProcessor<T> chunkProcessor) {
		if (chunkProcessor instanceof FaultTolerantChunkProcessor<?, ?>) {
			((FaultTolerantChunkProcessor<?, ?>) chunkProcessor).setBuffering(false);
		}
	}

	/**
	 * Replace the chunk processor in the tasklet provided with one that can act as a master in the Remote Chunking
	 * pattern.
	 * 
	 * @param tasklet a ChunkOrientedTasklet
	 * @param chunkWriter an ItemWriter that can send the chunks to remote workers
	 * @param stepContributionSource a StepContributionSource used to gather results from the workers
	 */
	private void replaceChunkProcessor(ChunkOrientedTasklet<?> tasklet, ItemWriter<T> chunkWriter,
			final StepContributionSource stepContributionSource) {
		setField(tasklet, "chunkProcessor", new SimpleChunkProcessor<T, T>(new PassThroughItemProcessor<T>(),
				chunkWriter) {
			@Override
			protected void write(StepContribution contribution, Chunk<T> inputs, Chunk<T> outputs) throws Exception {
				doWrite(outputs.getItems());
				// Do not update the step contribution until the chunks are
				// actually processed
				updateStepContribution(contribution, stepContributionSource);
			}
		});
	}

	/**
	 * Update a StepContribution with all the data from a StepContributionSource. The filter and write counts plus the
	 * exit status will be updated to reflect the data in the source.
	 * 
	 * @param contribution the current contribution
	 * @param stepContributionSource a source of StepContributions
	 */
	protected void updateStepContribution(StepContribution contribution, StepContributionSource stepContributionSource) {
		for (StepContribution result : stepContributionSource.getStepContributions()) {
			contribution.incrementFilterCount(result.getFilterCount());
			contribution.incrementWriteCount(result.getWriteCount());
			for (int i = 0; i < result.getProcessSkipCount(); i++) {
				contribution.incrementProcessSkipCount();
			}
			for (int i = 0; i < result.getWriteSkipCount(); i++) {
				contribution.incrementWriteSkipCount();
			}
			contribution.setExitStatus(contribution.getExitStatus().and(result.getExitStatus()));
		}
	}

	/**
	 * Pull out an item writer from a ChunkProcessor
	 * @param chunkProcessor a ChunkProcessor
	 * @return its ItemWriter
	 */
	@SuppressWarnings("unchecked")
	private ItemWriter<T> getItemWriter(ChunkProcessor<T> chunkProcessor) {
		return (ItemWriter<T>) getField(chunkProcessor, "itemWriter");
	}

	/**
	 * Pull the ChunkProcessor out of a tasklet.
	 * @param tasklet a ChunkOrientedTasklet
	 * @return the ChunkProcessor
	 */
	@SuppressWarnings("unchecked")
	private ChunkProcessor<T> getChunkProcessor(ChunkOrientedTasklet<?> tasklet) {
		return (ChunkProcessor<T>) getField(tasklet, "chunkProcessor");
	}

	/**
	 * Pull a Tasklet out of a step.
	 * @param step a TaskletStep
	 * @return the Tasklet
	 */
	private Tasklet getTasklet(TaskletStep step) {
		return (Tasklet) getField(step, "tasklet");
	}

	private static Object getField(Object target, String name) {
		Assert.notNull(target, "Target object must not be null");
		Field field = ReflectionUtils.findField(target.getClass(), name);
		if (field == null) {
			logger.debug("Could not find field [" + name + "] on target [" + target + "]");
			return null;
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Getting field [" + name + "] from target [" + target + "]");
		}
		ReflectionUtils.makeAccessible(field);
		return ReflectionUtils.getField(field, target);
	}

	private static void setField(Object target, String name, Object value) {
		Assert.notNull(target, "Target object must not be null");
		Field field = ReflectionUtils.findField(target.getClass(), name);
		if (field == null) {
			throw new IllegalStateException("Could not find field [" + name + "] on target [" + target + "]");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Getting field [" + name + "] from target [" + target + "]");
		}
		ReflectionUtils.makeAccessible(field);
		ReflectionUtils.setField(field, target, value);
	}

}
