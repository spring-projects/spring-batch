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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.core.convert.converter.Converter;

/**
 * {@link Converter} implementation from {@link LocalDateTime} to {@link String}.
 * <p>
 * This converter formats dates according to the
 * {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME} format.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.0.1
 */
public class LocalDateTimeToStringConverter extends AbstractDateTimeConverter
		implements Converter<LocalDateTime, String> {

	@Override
	public String convert(LocalDateTime source) {
		return source.format(super.localDateTimeFormatter);
	}

}
