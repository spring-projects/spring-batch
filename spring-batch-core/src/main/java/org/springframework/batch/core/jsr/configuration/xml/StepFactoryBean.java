/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.xml;

import javax.batch.api.Batchlet;
import javax.batch.api.chunk.CheckpointAlgorithm;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.api.chunk.ItemReader;
import javax.batch.api.chunk.ItemWriter;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.xml.StepParserStepFactoryBean;
import org.springframework.batch.core.jsr.step.batchlet.BatchletAdapter;
import org.springframework.batch.core.jsr.step.builder.JsrFaultTolerantStepBuilder;
import org.springframework.batch.core.jsr.step.builder.JsrSimpleStepBuilder;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.jsr.item.ItemProcessorAdapter;
import org.springframework.batch.jsr.item.ItemReaderAdapter;
import org.springframework.batch.jsr.item.ItemWriterAdapter;
import org.springframework.batch.jsr.repeat.CheckpointAlgorithmAdapter;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.beans.factory.FactoryBean;

/**
 * This {@link FactoryBean} is used by the JSR-352 namespace parser to create
 * {@link Step} objects. It stores all of the properties that are
 * configurable on the &lt;step/&gt;.
 *
 * @author Michael Minella
 * @since 3.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class StepFactoryBean extends StepParserStepFactoryBean {

	/**
	 * Wraps a {@link Batchlet} in a {@link BatchletAdapter} if required for consumption
	 * by the rest of the framework.
	 *
	 * @param tasklet {@link Tasklet} or {@link Batchlet} implementation
	 * @throws IllegalArgumentException if tasklet does not implement either Tasklet or Batchlet
	 */
	public void setTasklet(Object tasklet) {
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
	public void setItemReader(Object itemReader) {
		if(itemReader instanceof org.springframework.batch.item.ItemReader) {
			super.setItemReader((org.springframework.batch.item.ItemReader) itemReader);
		} else if(itemReader instanceof ItemReader){
			super.setItemReader(new ItemReaderAdapter((ItemReader) itemReader));
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
	public void setItemProcessor(Object itemProcessor) {
		if(itemProcessor instanceof org.springframework.batch.item.ItemProcessor) {
			super.setItemProcessor((org.springframework.batch.item.ItemProcessor) itemProcessor);
		} else if(itemProcessor instanceof ItemProcessor){
			super.setItemProcessor(new ItemProcessorAdapter((ItemProcessor)itemProcessor));
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
	public void setItemWriter(Object itemWriter) {
		if(itemWriter instanceof org.springframework.batch.item.ItemWriter) {
			super.setItemWriter((org.springframework.batch.item.ItemWriter) itemWriter);
		} else if(itemWriter instanceof ItemWriter){
			super.setItemWriter(new ItemWriterAdapter((ItemWriter) itemWriter));
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
	public void setChunkCompletionPolicy(Object chunkCompletionPolicy) {
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
	protected FaultTolerantStepBuilder getFaultTolerantStepBuilder(String stepName) {
		return new JsrFaultTolerantStepBuilder(new StepBuilder(stepName));
	}

	@Override
	protected SimpleStepBuilder getSimpleStepBuilder(String stepName) {
		return new JsrSimpleStepBuilder(new StepBuilder(stepName));
	}
}
