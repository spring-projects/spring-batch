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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Dave Syer
 * 
 */
@RunWith(Parameterized.class)
public class DateFormatTests {

	private final SimpleDateFormat format;

	private final String value;

	private final int hour;

	/**
	 * 
	 */
	public DateFormatTests(String pattern, String value, int hour) {
		this.format = new SimpleDateFormat(pattern);
		this.value = value;
		this.hour = hour;
	}

	@Test
	public void testDateFormat() throws Exception {
		Date date = format.parse(value);
		System.err.println(format.toPattern() + " + " + value + " --> " + date);
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		assertEquals(hour, calendar.get(Calendar.HOUR_OF_DAY));
	}

	@Parameters
	public static List<Object[]> data() {
		List<Object[]> params = new ArrayList<Object[]>();
		params.add(new Object[] { "yyyy-MM-dd HH:mm:ss.S z", "1970-01-01 11:20:34.0 GMT", 12 });
		params.add(new Object[] { "yyyy-MM-dd HH:mm:ss.S z", "1971-02-01 11:20:34.0 GMT", 12 });
		// After 1972 you get the right answer
		params.add(new Object[] { "yyyy-MM-dd HH:mm:ss.S z", "1972-02-01 11:20:34.0 GMT", 11 });
		params.add(new Object[] { "yyyy-MM-dd HH:mm:ss.S z", "1976-02-01 11:20:34.0 GMT", 11 });
		params.add(new Object[] { "yyyy-MM-dd HH:mm:ss.S z", "1982-02-01 11:20:34.0 GMT", 11 });
		params.add(new Object[] { "yyyy-MM-dd HH:mm:ss.S z", "2008-02-01 11:20:34.0 GMT", 11 });
		return params;
	}

}
