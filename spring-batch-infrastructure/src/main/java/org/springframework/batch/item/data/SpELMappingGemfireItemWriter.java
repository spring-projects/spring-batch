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

import org.springframework.batch.item.SpELItemKeyMapper;
import org.springframework.util.Assert;

/**
 * A convenient {@link GemfireItemWriter} implementation that uses a {@link SpELItemKeyMapper}
 * 
 * @author David Turanski
 * @since 2.2
 */
public class SpELMappingGemfireItemWriter<K, V> extends GemfireItemWriter<K, V> {
	/**
	 * A constructor that accepts a SpEL expression used to derive the key
	 * @param keyExpression
	 */
	SpELMappingGemfireItemWriter(String keyExpression) {
		super();
		Assert.hasText(keyExpression, "a valid keyExpression is required.");
		setItemKeyMapper(new SpELItemKeyMapper<>(keyExpression));
	}
}
