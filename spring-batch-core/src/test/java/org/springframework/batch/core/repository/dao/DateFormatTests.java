/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.repository.dao;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test case showing some weirdnesses in date formatting. Looks like a bug in
 * SimpleDateFormat / GregorianCalendar, and it affects the JSON deserialization
 * that we use in the ExecutionContext around daylight savings.
 * 
 * @author Dave Syer
 * 
 */
@RunWith(Parameterized.class)
public class DateFormatTests {

	private final SimpleDateFormat format;

	private final String input;

	private final int hour;

	private final String output;

	/**
	 * 
	 */
	public DateFormatTests(String pattern, String input, String output, int hour) {
		this.output = output;
		this.format = new SimpleDateFormat(pattern, Locale.UK);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		this.input = input;
		this.hour = hour;
	}

	@Test
	public void testDateFormat() throws Exception {

		Date date = format.parse(input);
		GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"), Locale.UK);
		calendar.setTime(date);

		// System.err.println(format.toPattern() + " + " + input + " --> " +
		// calendar.getTime());

		// This assertion is true...
		assertEquals(hour, calendar.get(Calendar.HOUR_OF_DAY));
		// ...but the toString() does not match in 1970 and 1971
		assertEquals(output, format.format(date));

	}

	@Parameters
	public static List<Object[]> data() {

		List<Object[]> params = new ArrayList<Object[]>();
		String format = "yyyy-MM-dd HH:mm:ss.S z";

		/*
		 * When the date format has an explicit time zone these are OK. But on
		 * 2008/10/26 when the clocks went back to GMT these failed the hour
		 * assertion (with the hour coming back as 12). On 2008/10/27, the day
		 * after, they are fine, but the toString still didn't match.
		 */
		params.add(new Object[] { format, "1970-01-01 11:20:34.0 GMT", "1970-01-01 11:20:34.0 GMT", 11 });
		params.add(new Object[] { format, "1971-02-01 11:20:34.0 GMT", "1971-02-01 11:20:34.0 GMT", 11 });

		// After 1972 you always get the right answer
		params.add(new Object[] { format, "1972-02-01 11:20:34.0 GMT", "1972-02-01 11:20:34.0 GMT", 11 });
		params.add(new Object[] { format, "1976-02-01 11:20:34.0 GMT", "1976-02-01 11:20:34.0 GMT", 11 });
		params.add(new Object[] { format, "1982-02-01 11:20:34.0 GMT", "1982-02-01 11:20:34.0 GMT", 11 });
		params.add(new Object[] { format, "2008-02-01 11:20:34.0 GMT", "2008-02-01 11:20:34.0 GMT", 11 });

		return params;

	}

}
