/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.infrastructure.item.file.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.file.transform.DefaultFieldSetFactory;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;

/**
 * @author Dave Syer
 *
 */
class DefaultFieldSetFactoryTests {

	private final DefaultFieldSetFactory factory = new DefaultFieldSetFactory();

	@Test
	void testVanillaFieldSet() {
		FieldSet fieldSet = factory.create(new String[] { "foo", "bar" });
		assertEquals("foo", fieldSet.readString(0));
	}

	@Test
	void testVanillaFieldSetWithNames() {
		FieldSet fieldSet = factory.create(new String[] { "1", "bar" }, new String[] { "foo", "bar" });
		assertEquals(1, fieldSet.readInt("foo"));
	}

	@Test
	void testFieldSetWithDateFormat() throws Exception {
		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
		factory.setDateFormat(format);
		FieldSet fieldSet = factory.create(new String[] { "1999/12/18", "bar" });
		assertEquals(format.parse("1999/12/18"), fieldSet.readDate(0));
	}

	@Test
	void testFieldSetWithNumberFormat() {
		factory.setNumberFormat(NumberFormat.getNumberInstance(Locale.GERMAN));
		FieldSet fieldSet = factory.create(new String[] { "19.991.218", "bar" });
		assertEquals(19991218, fieldSet.readInt(0));
	}

}
