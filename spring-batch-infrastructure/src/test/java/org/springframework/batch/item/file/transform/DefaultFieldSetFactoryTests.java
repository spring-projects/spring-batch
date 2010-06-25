/*
 * Copyright 2006-2010 the original author or authors.
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
package org.springframework.batch.item.file.transform;

import static org.junit.Assert.assertEquals;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.junit.Test;

/**
 * @author Dave Syer
 * 
 */
public class DefaultFieldSetFactoryTests {

	private DefaultFieldSetFactory factory = new DefaultFieldSetFactory();

	@Test
	public void testVanillaFieldSet() throws Exception {
		FieldSet fieldSet = factory.create(new String[] {"foo", "bar"} );
		assertEquals("foo", fieldSet.readString(0));
	}

	@Test
	public void testVanillaFieldSetWithNames() throws Exception {
		FieldSet fieldSet = factory.create(new String[] {"1", "bar"}, new String[] {"foo", "bar"} );
		assertEquals(1, fieldSet.readInt("foo"));
	}

	@Test
	public void testFieldSetWithDateFormat() throws Exception {
		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
		factory.setDateFormat(format);
		FieldSet fieldSet = factory.create(new String[] {"1999/12/18", "bar"} );
		assertEquals(format.parse("1999/12/18"), fieldSet.readDate(0));
	}

	@Test
	public void testFieldSetWithNumberFormat() throws Exception {
		factory.setNumberFormat(NumberFormat.getNumberInstance(Locale.GERMAN));
		FieldSet fieldSet = factory.create(new String[] {"19.991.218", "bar"} );
		assertEquals(19991218, fieldSet.readInt(0));
	}

}
