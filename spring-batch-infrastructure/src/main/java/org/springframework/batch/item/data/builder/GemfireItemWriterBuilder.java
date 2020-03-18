/*
 * Copyright 2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.data.builder;

import org.springframework.batch.item.data.GemfireItemWriter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.util.Assert;

/**
 * A builder implementation for the {@link GemfireItemWriter}
 *
 * @author Glenn Renfro
 * @since 4.0
 * @see GemfireItemWriterBuilder
 */
public class GemfireItemWriterBuilder<K, V> {

	private GemfireTemplate template;

	private Converter<V, K> itemKeyMapper;

	private boolean delete;

	/**
	 * Establishes the GemfireTemplate the writer should use.
	 * @param template the {@link GemfireTemplate} to set.
	 * @return The current instance of the builder.
	 * @see GemfireItemWriter#setTemplate(GemfireTemplate)
	 */
	public GemfireItemWriterBuilder<K, V> template(GemfireTemplate template) {
		this.template = template;

		return this;
	}

	/**
	 * Set the {@link Converter} to use to derive the key from the item.
	 *
	 * @param itemKeyMapper the Converter to use.
	 * @return The current instance of the builder.
	 * @see GemfireItemWriter#setItemKeyMapper(Converter)
	 */
	public GemfireItemWriterBuilder<K, V> itemKeyMapper(Converter<V, K> itemKeyMapper) {
		this.itemKeyMapper = itemKeyMapper;

		return this;
	}

	/**
	 * Indicates if the items being passed to the writer are to be saved or removed from
	 * the data store. If set to false (default), the items will be saved. If set to true,
	 * the items will be removed.
	 *
	 * @param delete removal indicator.
	 * @return The current instance of the builder.
	 * @see GemfireItemWriter#setDelete(boolean)
	 */
	public GemfireItemWriterBuilder<K, V> delete(boolean delete) {
		this.delete = delete;

		return this;
	}


	/**
	 * Validates and builds a {@link GemfireItemWriter}.
	 *
	 * @return a {@link GemfireItemWriter}
	 */
	public GemfireItemWriter<K, V> build() {
		Assert.notNull(this.template, "template is required.");
		Assert.notNull(this.itemKeyMapper, "itemKeyMapper is required.");

		GemfireItemWriter<K, V> writer = new GemfireItemWriter<>();
		writer.setTemplate(this.template);
		writer.setItemKeyMapper(this.itemKeyMapper);
		writer.setDelete(this.delete);
		return writer;
	}
}
