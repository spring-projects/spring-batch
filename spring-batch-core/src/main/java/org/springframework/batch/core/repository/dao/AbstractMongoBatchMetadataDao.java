/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.core.repository.dao;

import org.springframework.util.Assert;

/**
 * Encapsulates common functionality needed by MongoDB batch metadata DAOs - handles
 * collection prefixes.
 *
 * @author Myeongha Shin
 * @author Mahmoud Ben Hassine
 * @since 6.1.0
 */
public abstract class AbstractMongoBatchMetadataDao {

	/**
	 * Default value for the collection prefix property.
	 */
	public static final String DEFAULT_COLLECTION_PREFIX = "BATCH_";

	/**
	 * Default name of the collection holding the sequences, without the collection
	 * prefix.
	 */
	public static final String DEFAULT_SEQUENCES_COLLECTION_NAME = "SEQUENCES";

	/**
	 * Default name of the job instance incrementer, without the collection prefix.
	 */
	public static final String DEFAULT_JOB_INSTANCE_INCREMENTER_NAME = "JOB_INSTANCE_SEQ";

	/**
	 * Default name of the job execution incrementer, without the collection prefix.
	 */
	public static final String DEFAULT_JOB_EXECUTION_INCREMENTER_NAME = "JOB_EXECUTION_SEQ";

	/**
	 * Default name of the step execution incrementer, without the collection prefix.
	 */
	public static final String DEFAULT_STEP_EXECUTION_INCREMENTER_NAME = "STEP_EXECUTION_SEQ";

	private String collectionPrefix = DEFAULT_COLLECTION_PREFIX;

	/**
	 * Prepend the configured collection prefix to the given collection name.
	 * @param collectionName the collection name, without the collection prefix
	 * @return the fully qualified collection name
	 */
	protected String getCollectionName(String collectionName) {
		return this.collectionPrefix + collectionName;
	}

	/**
	 * Prepend the configured collection prefix to the given sequence name.
	 * @param sequenceName the sequence name, without the collection prefix
	 * @return the fully qualified sequence name
	 */
	protected String getSequenceName(String sequenceName) {
		return this.collectionPrefix + sequenceName;
	}

	protected String getCollectionPrefix() {
		return this.collectionPrefix;
	}

	/**
	 * Public setter for the collection prefix property. This will be prepended to all the
	 * collection names before queries are executed. Defaults to
	 * {@link #DEFAULT_COLLECTION_PREFIX}.
	 * @param collectionPrefix the collection prefix to set
	 */
	public void setCollectionPrefix(String collectionPrefix) {
		Assert.notNull(collectionPrefix, "Collection prefix must not be null.");
		this.collectionPrefix = collectionPrefix;
	}

}
