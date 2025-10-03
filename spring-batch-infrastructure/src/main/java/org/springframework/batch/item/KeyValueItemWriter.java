/*
 * Copyright 2002-2025 the original author or authors.
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
package org.springframework.batch.item;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

/**
 * A base class to implement any {@link ItemWriter} that writes to a key value store using
 * a {@link Converter} to derive a key from an item. If a derived key is null, the item
 * will be skipped and a warning logged.
 *
 * @author David Turanski
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 * @since 2.2
 *
 */
public abstract class KeyValueItemWriter<K, V> implements ItemWriter<V>, InitializingBean {

	private static final Log logger = LogFactory.getLog(KeyValueItemWriter.class);

	protected Converter<V, K> itemKeyMapper;

	protected boolean delete;

	/**
	 * Create a new {@link KeyValueItemWriter} instance.
	 * @param itemKeyMapper the {@link Converter} used to derive a key from an item.
	 * @since 6.0
	 */
	public KeyValueItemWriter(Converter<V, K> itemKeyMapper) {
		Assert.notNull(itemKeyMapper, "itemKeyMapper must not be null");
		this.itemKeyMapper = itemKeyMapper;
	}

	@Override
	public void write(Chunk<? extends V> chunk) throws Exception {
		for (V item : chunk) {
			K key = itemKeyMapper.convert(item);
			// TODO should we add a strict mode and throw an exception instead?
			if (key == null) {
				logger.warn("Derived Key is null for item = " + item + ". This item will be skipped.");
				continue;
			}
			writeKeyValue(key, item);
		}
		flush();
	}

	/**
	 * Flush items to the key/value store.
	 * @throws Exception if unable to flush items
	 */
	protected void flush() throws Exception {
	}

	/**
	 * Subclasses implement this method to write each item to key value store
	 * @param key the key
	 * @param value the item
	 */
	protected abstract void writeKeyValue(K key, V value);

	/**
	 * afterPropertiesSet() hook
	 */
	protected abstract void init();

	/**
	 * Set the {@link Converter} to use to derive the key from the item
	 * @param itemKeyMapper the {@link Converter} used to derive a key from an item.
	 */
	public void setItemKeyMapper(Converter<V, K> itemKeyMapper) {
		this.itemKeyMapper = itemKeyMapper;
	}

	/**
	 * Sets the delete flag to have the item writer perform deletes
	 * @param delete if true {@link ItemWriter} will perform deletes, if false not to
	 * perform deletes.
	 */
	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		init();
	}

}
