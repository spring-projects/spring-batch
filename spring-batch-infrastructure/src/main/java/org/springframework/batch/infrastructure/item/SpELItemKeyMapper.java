/*
 * Copyright 2002-present the original author or authors.
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
package org.springframework.batch.infrastructure.item;

import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * An implementation of {@link Converter} that uses SpEL to map a Value to a key
 *
 * @author David Turanski
 * @author Stefano Cordio
 * @author Mahmoud Ben Hassine
 * @since 2.2
 */
public class SpELItemKeyMapper<K, V> implements Converter<V, K> {

	private final Expression parsedExpression;

	public SpELItemKeyMapper(String keyExpression) {
		parsedExpression = new SpelExpressionParser().parseExpression(keyExpression);
	}

	@SuppressWarnings("unchecked")
	@Override
	public K convert(V item) {
		K key = (K) parsedExpression.getValue(item);
		if (key == null) {
			throw new IllegalArgumentException(
					"Derived Key is null for item = " + item + ". This item will be skipped.");
		}
		return key;
	}

}
