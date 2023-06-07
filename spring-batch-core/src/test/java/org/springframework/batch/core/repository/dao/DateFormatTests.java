/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.core.repository.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test case showing some weirdnesses in date formatting. Looks like a bug in
 * SimpleDateFormat / GregorianCalendar, and it affects the JSON deserialization that we
 * use in the ExecutionContext around daylight savings.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class DateFormatTests {

	private SimpleDateFormat format;

	@BeforeEach
	void setUp() {
		format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S z", Locale.UK);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	@MethodSource
	@ParameterizedTest
	void testDateFormat(String input, String output, int hour) throws Exception {
		Date date = format.parse(input);
		GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"), Locale.UK);
		calendar.setTime(date);

		// calendar.getTime());

		// This assertion is true...
		assertEquals(hour, calendar.get(Calendar.HOUR_OF_DAY));
		// ...but the toString() does not match in 1970 and 1971
		assertEquals(output, format.format(date));

	}

	static Stream<Arguments> testDateFormat() {
		/*
		 * When the date format has an explicit time zone these are OK. But on 2008/10/26
		 * when the clocks went back to GMT these failed the hour assertion (with the hour
		 * coming back as 12). On 2008/10/27, the day after, they are fine, but the
		 * toString still didn't match.
		 */
		return Stream.of(Arguments.of("1970-01-01 11:20:34.0 GMT", "1970-01-01 11:20:34.0 GMT", 11),
				Arguments.of("1971-02-01 11:20:34.0 GMT", "1971-02-01 11:20:34.0 GMT", 11),
				// After 1972 you always get the right answer
				Arguments.of("1972-02-01 11:20:34.0 GMT", "1972-02-01 11:20:34.0 GMT", 11),
				Arguments.of("1976-02-01 11:20:34.0 GMT", "1976-02-01 11:20:34.0 GMT", 11),
				Arguments.of("1982-02-01 11:20:34.0 GMT", "1982-02-01 11:20:34.0 GMT", 11),
				Arguments.of("2008-02-01 11:20:34.0 GMT", "2008-02-01 11:20:34.0 GMT", 11));
	}

}
