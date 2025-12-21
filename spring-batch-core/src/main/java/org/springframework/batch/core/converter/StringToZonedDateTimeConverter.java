/*
 * Copyright 2026-present the original author or authors.
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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.core.convert.converter.Converter;

/**
 * {@link Converter} implementation from {@link String} to {@link ZonedDateTime}.
 * <p>
 * This converter expects strings in the {@link DateTimeFormatter#ISO_ZONED_DATE_TIME}
 * format.
 *
 * @author Eunbin Son
 * @since 6.0.2
 */
public class StringToZonedDateTimeConverter implements Converter<String, ZonedDateTime> {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

	@Override
	public ZonedDateTime convert(String source) {
		return ZonedDateTime.parse(source, FORMATTER);
	}

}
