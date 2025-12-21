/*
 * Copyright 2023-2025 the original author or authors.
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for {@link OffsetDateTimeToStringConverter}.
 *
 * @author Eunbin Son
 */
class OffsetDateTimeToStringConverterTests {

	private final OffsetDateTimeToStringConverter converter = new OffsetDateTimeToStringConverter();

	@Test
	void testConvert() {
		// given
		OffsetDateTime offsetDateTime = OffsetDateTime.of(2023, 12, 25, 10, 30, 0, 0, ZoneOffset.of("+09:00"));

		// when
		String converted = converter.convert(offsetDateTime);

		// then
		assertEquals("2023-12-25T10:30:00+09:00", converted);
	}

	@Test
	void testConvertWithUTC() {
		// given
		OffsetDateTime offsetDateTime = OffsetDateTime.of(2023, 12, 25, 10, 30, 0, 0, ZoneOffset.UTC);

		// when
		String converted = converter.convert(offsetDateTime);

		// then
		assertEquals("2023-12-25T10:30:00Z", converted);
	}

}
