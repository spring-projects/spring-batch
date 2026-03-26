/*
 * Copyright 2026 the original author or authors.
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
 * @since 6.1
 */
public abstract class AbstractMongoBatchMetadataDao {

	/**
	 * Default value for the collection prefix property.
	 */
	public static final String DEFAULT_COLLECTION_PREFIX = "BATCH_";

	private String collectionPrefix = DEFAULT_COLLECTION_PREFIX;

	protected String getCollectionPrefix() {
		return this.collectionPrefix;
	}

	/**
	 * Public setter for the collection prefix property. This will be prefixed to all the
	 * collection names before queries are executed. Defaults to
	 * {@link #DEFAULT_COLLECTION_PREFIX}.
	 * @param collectionPrefix the collection prefix to set
	 */
	public void setCollectionPrefix(String collectionPrefix) {
		Assert.notNull(collectionPrefix, "Collection prefix must not be null.");
		this.collectionPrefix = collectionPrefix;
	}

}
