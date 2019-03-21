/*
 * Copyright 2002-2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * https://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.batch.item.data;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.KeyValueItemWriter;
import org.springframework.data.gemfire.GemfireOperations;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.util.Assert;

/**
 * An {@link ItemWriter} that stores items in GemFire
 * 
 * @author David Turanski
 * @since 2.2
 *
 */
public class GemfireItemWriter<K,V> extends KeyValueItemWriter<K,V> {
	private GemfireOperations gemfireTemplate;
	/**
	 * @param gemfireTemplate the {@link GemfireTemplate} to set
	 */
	public void setTemplate(GemfireTemplate gemfireTemplate) {
		this.gemfireTemplate = gemfireTemplate;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.KeyValueItemWriter#writeKeyValue(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected void writeKeyValue(K key, V value) {
		if (delete) {
			gemfireTemplate.remove(key);
		} else {
			gemfireTemplate.put(key, value);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.KeyValueItemWriter#init()
	 */
	@Override
	protected void init() {
		Assert.notNull(gemfireTemplate, "A GemfireTemplate is required.");
	}

}
