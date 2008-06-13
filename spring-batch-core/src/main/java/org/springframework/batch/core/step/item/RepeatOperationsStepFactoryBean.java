/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.core.step.item;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.support.RepeatTemplate;

/**
 * Factory bean for {@link Step} implementations allowing registration of
 * listeners and also direct injection of the {@link RepeatOperations} needed at
 * step and chunk level.
 * 
 * @author Dave Syer
 * 
 */
public class RepeatOperationsStepFactoryBean extends AbstractStepFactoryBean {

	private StepListener[] listeners = new StepListener[0];

	private RepeatOperations chunkOperations = new RepeatTemplate();

	private RepeatOperations stepOperations = new RepeatTemplate();


	/**
	 * The listeners to inject into the {@link Step}. Any instance of
	 * {@link StepListener} can be used, and will then receive callbacks at the
	 * appropriate stage in the step.
	 * 
	 * @param listeners an array of listeners
	 */
	public void setListeners(StepListener[] listeners) {
		this.listeners = listeners;
	}

	/**
	 * The {@link RepeatOperations} to use for the outer loop of the batch
	 * processing. Should be set up by the caller through a factory. Defaults to
	 * a plain {@link RepeatTemplate}.
	 * 
	 * @param stepOperations a {@link RepeatOperations} instance.
	 */
	public void setStepOperations(RepeatOperations stepOperations) {
		this.stepOperations = stepOperations;
	}

	/**
	 * The {@link RepeatOperations} to use for the inner loop of the batch
	 * processing. should be set up by the caller through a factory. defaults to
	 * a plain {@link RepeatTemplate}.
	 * 
	 * @param chunkOperations a {@link RepeatOperations} instance.
	 */
	public void setChunkOperations(RepeatOperations chunkOperations) {
		this.chunkOperations = chunkOperations;
	}

	/**
	 * @param step
	 * 
	 */
	protected void applyConfiguration(ItemOrientedStep step) {

		super.applyConfiguration(step);

		ItemReader itemReader = getItemReader();
		ItemWriter itemWriter = getItemWriter();

		BatchListenerFactoryHelper helper = new BatchListenerFactoryHelper();

		StepExecutionListener[] stepListeners = helper.getStepListeners(listeners);
		itemReader = helper.getItemReader(itemReader, listeners);
		itemWriter = helper.getItemWriter(itemWriter, listeners);
		RepeatOperations chunkOperations = helper.addChunkListeners(this.chunkOperations, listeners);

		// In case they are used by subclasses:
		setItemReader(itemReader);
		setItemWriter(itemWriter);

		step.setStepExecutionListeners(stepListeners);
		step.setItemHandler(new SimpleItemHandler(itemReader, itemWriter));

		step.setChunkOperations(chunkOperations);
		step.setStepOperations(stepOperations);

	}

}
