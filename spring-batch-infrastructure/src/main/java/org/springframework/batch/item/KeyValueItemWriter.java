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
package org.springframework.batch.item;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

/**
 * A base class to implement any {@link ItemWriter} that writes to a key value store 
 * using a {@link Converter} to derive a key from an item
 * 
 * @author David Turanski
 * @since 2.2
 *
 */
public abstract class KeyValueItemWriter<K, V> implements ItemWriter<V>, InitializingBean {

	protected Converter<V, K> itemKeyMapper;
	protected boolean delete;

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemWriter#write(java.util.List)
	 */
	@Override
	public void write(List<? extends V> items) throws Exception {
		if (items == null) {
			return;
		}
		for (V item : items) {
			K key = itemKeyMapper.convert(item);
			writeKeyValue(key, item);
		}
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
	 *
	 * @param itemKeyMapper the {@link Converter} used to derive a key from an item.
	 */
	public void setItemKeyMapper(Converter<V, K> itemKeyMapper) {
		this.itemKeyMapper = itemKeyMapper;
	}

	/**
	 * Sets the delete flag to have the item writer perform deletes
	 *
	 * @param delete if true {@link ItemWriter} will perform deletes,
	 * 		if false not to perform deletes.
	 */
	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(itemKeyMapper, "itemKeyMapper requires a Converter type.");
		init();
	}
}
