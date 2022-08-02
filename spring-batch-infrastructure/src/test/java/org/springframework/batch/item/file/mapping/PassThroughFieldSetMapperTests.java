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
package org.springframework.batch.item.file.mapping;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dave Syer
 *
 */
class PassThroughFieldSetMapperTests {

	private final PassThroughFieldSetMapper mapper = new PassThroughFieldSetMapper();

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper#mapFieldSet(org.springframework.batch.item.file.transform.FieldSet)}.
	 */
	@Test
	void testMapLine() {
		FieldSet fieldSet = new DefaultFieldSet(new String[] { "foo", "bar" });
		assertEquals(fieldSet, mapper.mapFieldSet(fieldSet));
	}

}
