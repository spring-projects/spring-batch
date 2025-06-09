/*
 * Copyright 2002-2023 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * An implementation of {@link Converter} that uses SpEL to map a Value to a key
 *
 * @author David Turanski
 * @author Stefano Cordio
 * @since 2.2
 */
public class SpELItemKeyMapper<K, V> implements Converter<V, K> {

	private final Expression parsedExpression;

	public SpELItemKeyMapper(String keyExpression) {
		parsedExpression = new SpelExpressionParser().parseExpression(keyExpression);
	}

	@SuppressWarnings("unchecked")
	@Override
	public @Nullable K convert(V item) {
		return (K) parsedExpression.getValue(item);
	}

}
