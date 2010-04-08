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
import org.springframework.batch.core.step.item.SimpleChunkProcessor;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Convenient factory bean for a chunk handler that also converts an existing
 * chunk-oriented step into a remote chunk master. The idea is to lift the
 * existing chunk processor out of a step that works locally, and replace it
 * with a chunk writer that is already configured to write chunks into a message
 * channel. The existing step hands its business chunk processing responsibility
 * over to the handler produced by the factory, which then needs to be set up as
 * a remote worker on the other end of the channel the chunks are being sent to.
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
	 * @param step the step to set
	 */
	public void setStep(TaskletStep step) {
		this.step = step;
	}

	/**
	 * @param chunkWriter the chunk writer to set
	 */
	public void setChunkWriter(ItemWriter<T> chunkWriter) {
		this.chunkWriter = chunkWriter;
	}

	/**
	 * @param stepContributionSource the step contribution source to set
	 * (defaults to the chunk writer)
	 */
	public void setStepContributionSource(StepContributionSource stepContributionSource) {
		this.stepContributionSource = stepContributionSource;
	}

	public Class<?> getObjectType() {
		return ChunkHandler.class;
	}

	public boolean isSingleton() {
		return true;
	}

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
		handler.setChunkProcessor(chunkProcessor);
		// TODO: create step context for the processor in case it has scope="step" dependencies
		handler.afterPropertiesSet();

		return handler;

	}

	/**
	 * @param tasklet
	 * @param chunkWriter
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
	 * @param contribution
	 * @param chunkWriter
	 */
	private void updateStepContribution(StepContribution contribution, StepContributionSource stepContributionSource) {
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
	 * @param chunkProcessor
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private ItemWriter<T> getItemWriter(ChunkProcessor<T> chunkProcessor) {
		return (ItemWriter<T>) getField(chunkProcessor, "itemWriter");
	}

	/**
	 * @param tasklet
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private ChunkProcessor<T> getChunkProcessor(ChunkOrientedTasklet<?> tasklet) {
		return (ChunkProcessor<T>) getField(tasklet, "chunkProcessor");
	}

	/**
	 * @param bean
	 * @return
	 */
	private Tasklet getTasklet(TaskletStep bean) {
		return (Tasklet) getField(bean, "tasklet");
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
