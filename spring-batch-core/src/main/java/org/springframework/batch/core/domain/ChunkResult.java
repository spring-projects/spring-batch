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
package org.springframework.batch.core.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.enums.ShortCodedLabeledEnum;

/**
 * 
 * @author Ben Hale
 */
public class ChunkResult {

	public static final ChunkResultType SUCCESS = new ChunkResultType(0, "Success");

	public static final ChunkResultType FAILURE = new ChunkResultType(1, "Failure");

	private final ChunkResultType resultType;

	private final Long chunkId;

	private final List skippedItems;

	public ChunkResult(ChunkResultType resultType, Long chunkId, List skippedItems) {
		this.resultType = resultType;
		this.chunkId = chunkId;
		this.skippedItems = skippedItems;
	}

	public ChunkResultType getResultType() {
		return resultType;
	}

	public Long getChunkId() {
		return chunkId;
	}

	public List getSkippedItems() {
		return new ArrayList(skippedItems);
	}

	private static class ChunkResultType extends ShortCodedLabeledEnum {

		public ChunkResultType(int code, String label) {
			super(code, label);
		}

	}
}
