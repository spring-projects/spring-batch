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
package org.springframework.batch.core.jsr.configuration.support;

import javax.batch.api.Batchlet;
import javax.batch.api.Decider;
import javax.batch.api.chunk.CheckpointAlgorithm;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.api.chunk.ItemReader;
import javax.batch.api.chunk.ItemWriter;
import javax.batch.api.chunk.listener.ChunkListener;
import javax.batch.api.chunk.listener.ItemProcessListener;
import javax.batch.api.chunk.listener.ItemReadListener;
import javax.batch.api.chunk.listener.ItemWriteListener;
import javax.batch.api.chunk.listener.RetryProcessListener;
import javax.batch.api.chunk.listener.RetryReadListener;
import javax.batch.api.chunk.listener.RetryWriteListener;
import javax.batch.api.chunk.listener.SkipProcessListener;
import javax.batch.api.chunk.listener.SkipReadListener;
import javax.batch.api.chunk.listener.SkipWriteListener;
import javax.batch.api.listener.JobListener;
import javax.batch.api.listener.StepListener;
import javax.batch.api.partition.PartitionAnalyzer;
import javax.batch.api.partition.PartitionCollector;
import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionReducer;

/**
 * <p>
 * Simple enum representing metadata about batch artifacts and types.
 * </p>
 *
 * @author Chris Schaefer
 */
public enum BatchArtifact {
	ITEM_READER(ItemReader.class),
	ITEM_WRITER(ItemWriter.class),
	ITEM_PROCESSOR(ItemProcessor.class),
	CHECKPOINT_ALGORITHM(CheckpointAlgorithm.class),
	BATCHLET(Batchlet.class),
	ITEM_READ_LISTENER(ItemReadListener.class),
	ITEM_PROCESS_LISTENER(ItemProcessListener.class),
	ITEM_WRITE_LISTENER(ItemWriteListener.class),
	JOB_LISTENER(JobListener.class),
	STEP_LISTENER(StepListener.class),
	CHUNK_LISTENER(ChunkListener.class),
	SKIP_READ_LISTENER(SkipReadListener.class),
	SKIP_PROCESS_LISTENER(SkipProcessListener.class),
	SKIP_WRITER_LISTENER(SkipWriteListener.class),
	RETRY_READ_LISTENER(RetryReadListener.class),
	RETRY_PROCESS_LISTENER(RetryProcessListener.class),
	RETRY_WRITE_LISTENER(RetryWriteListener.class),
	PARTITION_MAPPER(PartitionMapper.class),
	PARTITION_REDUCER(PartitionReducer.class),
	PARTITION_COLLECTOR(PartitionCollector.class),
	PARTITION_ANALYZER(PartitionAnalyzer.class),
	PARTITION_PLAN(PartitionPlan.class),
	DECIDER(Decider.class);

	private Class<?> clazz;

	private BatchArtifact(Class<?> clazz) {
		this.clazz = clazz;
	}

	private Class<?> getBatchArtifactClass() {
		return clazz;
	}

	/**
	 * <p>
	 * Determines if the provided artifact is a JSR-352 batch artifact.
	 * </p>
	 *
	 * @param artifact the artifact to check
	 * @return boolean answer based on check
	 */
	public static boolean isBatchArtifact(Object artifact) {
		for (BatchArtifact batchArtifactType : BatchArtifact.values()) {
			if (batchArtifactType.getBatchArtifactClass().isInstance(artifact)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * <p>
	 * Enum to identify batch artifact types.
	 * </p>
	 *
	 * @author Chris Schaefer
	 * @since 3.0
	 */
	public enum BatchArtifactType {
		STEP,
		STEP_ARTIFACT,
		ARTIFACT,
		JOB
	}
}
