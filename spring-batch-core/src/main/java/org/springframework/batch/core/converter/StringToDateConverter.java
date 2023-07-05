/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.batch.core.converter;

import java.time.Instant;
import java.util.Date;

import org.springframework.core.convert.converter.Converter;

/**
 * {@link Converter} implementation from {@link String} to {@link java.util.Date}.
 * <p>
 * This converter expects strings in the
 * {@link java.time.format.DateTimeFormatter#ISO_INSTANT} format.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.0.1
 */
public class StringToDateConverter extends AbstractDateTimeConverter implements Converter<String, Date> {

	@Override
	public Date convert(String source) {
		return Date.from(super.instantFormatter.parse(source, Instant::from));
	}

}
