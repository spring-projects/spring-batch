/*
 * Copyright 2006-2019 the original author or authors.
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
package org.springframework.batch.core.listener;

import java.util.Arrays;
import java.util.Iterator;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;

/**
 * @author Lucas Ward
 * @author Dave Syer
 *
 */
public class CompositeStepExecutionListener implements StepExecutionListener {

	private OrderedComposite<StepExecutionListener> list = new OrderedComposite<>();

	/**
	 * Public setter for the listeners.
	 *
	 * @param listeners list of {@link StepExecutionListener}s to be called when step execution events occur.
	 */
	public void setListeners(StepExecutionListener[] listeners) {
		list.setItems(Arrays.asList(listeners));
	}

	/**
	 * Register additional listener.
	 *
	 * @param stepExecutionListener instance of {@link StepExecutionListener} to be registered.
	 */
	public void register(StepExecutionListener stepExecutionListener) {
		list.add(stepExecutionListener);
	}

	/**
	 * Call the registered listeners in reverse order, respecting and
	 * prioritizing those that implement {@link Ordered}.
	 * @see org.springframework.batch.core.StepExecutionListener#afterStep(StepExecution)
	 */
	@Nullable
	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		for (Iterator<StepExecutionListener> iterator = list.reverse(); iterator.hasNext();) {
			StepExecutionListener listener = iterator.next();
			ExitStatus close = listener.afterStep(stepExecution);
			stepExecution.setExitStatus(stepExecution.getExitStatus().and(close));
		}
		return stepExecution.getExitStatus();
	}

	/**
	 * Call the registered listeners in order, respecting and prioritizing those
	 * that implement {@link Ordered}.
	 * @see org.springframework.batch.core.StepExecutionListener#beforeStep(StepExecution)
	 */
	@Override
	public void beforeStep(StepExecution stepExecution) {
		for (Iterator<StepExecutionListener> iterator = list.iterator(); iterator.hasNext();) {
			StepExecutionListener listener = iterator.next();
			listener.beforeStep(stepExecution);
		}
	}

}
