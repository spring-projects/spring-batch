/*
 * Copyright 2025 the original author or authors.
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

import org.springframework.format.Formatter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * {@link Formatter} implementation for {@link Date}.
 * <p>
 * This formatter parses and prints dates according to the
 * {@link DateTimeFormatter#ISO_INSTANT} format.
 *
 * @author Stefano Cordio
 * @since 6.1.0
 */
class DateFormatter implements Formatter<Date> {

	private final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

	@Override
	public Date parse(String text, Locale locale) {
		return Date.from(formatter.parse(text, Instant::from));
	}

	@Override
	public String print(Date object, Locale locale) {
		return formatter.format(object.toInstant());
	}

}
