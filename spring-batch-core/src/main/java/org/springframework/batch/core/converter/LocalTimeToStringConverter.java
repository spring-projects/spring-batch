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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.core.convert.converter.Converter;

/**
 * {@link Converter} implementation from {@link LocalTime} to {@link String}.
 * <p>
 * This converter formats times according to the {@link DateTimeFormatter#ISO_LOCAL_TIME}
 * format.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.0.1
 * @deprecated since 6.1 in favor of
 * {@link ConversionServiceFactory#createConversionService()}. Scheduled for removal in
 * 6.3 or later.
 */
@Deprecated(since = "6.1", forRemoval = true)
public class LocalTimeToStringConverter extends AbstractDateTimeConverter implements Converter<LocalTime, String> {

	@Override
	public String convert(LocalTime source) {
		return source.format(super.localTimeFormatter);
	}

}
