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

package org.springframework.batch.item.file.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.file.transform.Name;

/**
 * @author Dan Garrette
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Injae Kim
 * @since 2.0
 */
class PatternMatchingCompositeLineMapperTests {

	private final PatternMatchingCompositeLineMapper<Name> mapper = new PatternMatchingCompositeLineMapper<>();

	@Test
	void testNoMappers() {
		mapper.setTokenizers(Collections.singletonMap("", (LineTokenizer) new DelimitedLineTokenizer()));
		Map<String, FieldSetMapper<Name>> fieldSetMappers = Collections.emptyMap();
		assertThrows(IllegalArgumentException.class, () -> mapper.setFieldSetMappers(fieldSetMappers));
	}

	@Test
	void testKeyFound() throws Exception {
		Map<String, LineTokenizer> tokenizers = new HashMap<>();
		tokenizers.put("foo*", line -> new DefaultFieldSet(new String[] { "a", "b" }));
		tokenizers.put("bar*", line -> new DefaultFieldSet(new String[] { "c", "d" }));
		tokenizers.put("regex.*", line -> new DefaultFieldSet(new String[] { "e", "f" }));
		mapper.setTokenizers(tokenizers);

		Map<String, FieldSetMapper<Name>> fieldSetMappers = new HashMap<>();
		fieldSetMappers.put("foo*", fs -> new Name(fs.readString(0), fs.readString(1), 0));
		fieldSetMappers.put("bar*", fs -> new Name(fs.readString(1), fs.readString(0), 0));
		mapper.setFieldSetMappers(fieldSetMappers);

		Name name = mapper.mapLine("bar", 1);
		assertEquals(new Name("d", "c", 0), name);
	}

	@Test
	void testKeyFoundByRegex() throws Exception {
		Map<String, LineTokenizer> tokenizers = new HashMap<>();
		tokenizers.put("foo*", line -> new DefaultFieldSet(new String[] { "a", "b" }));
		tokenizers.put("bar*", line -> new DefaultFieldSet(new String[] { "c", "d" }));
		tokenizers.put("regex.*", line -> new DefaultFieldSet(new String[] { "e", "f" }));
		mapper.setTokenizers(tokenizers);

		Map<String, FieldSetMapper<Name>> fieldSetMappers = new HashMap<>();
		fieldSetMappers.put("foo*", fs -> new Name(fs.readString(0), fs.readString(1), 0));
		fieldSetMappers.put("bar*", fs -> new Name(fs.readString(1), fs.readString(0), 0));
		fieldSetMappers.put("regex.*", fs -> new Name(fs.readString(0), fs.readString(1), 0));
		mapper.setFieldSetMappers(fieldSetMappers);

		Name name = mapper.mapLine("regex-ABC_12345", 1);
		assertEquals(new Name("e", "f", 0), name);
	}

	@Test
	void testMapperKeyNotFound() {
		Map<String, LineTokenizer> tokenizers = new HashMap<>();
		tokenizers.put("foo*", line -> new DefaultFieldSet(new String[] { "a", "b" }));
		tokenizers.put("bar*", line -> new DefaultFieldSet(new String[] { "c", "d" }));
		tokenizers.put("regex.*", line -> new DefaultFieldSet(new String[] { "e", "f" }));
		mapper.setTokenizers(tokenizers);

		Map<String, FieldSetMapper<Name>> fieldSetMappers = new HashMap<>();
		fieldSetMappers.put("foo*", fs -> new Name(fs.readString(0), fs.readString(1), 0));
		fieldSetMappers.put("regex.*", fs -> new Name(fs.readString(0), fs.readString(1), 0));
		mapper.setFieldSetMappers(fieldSetMappers);

		assertThrows(IllegalStateException.class, () -> mapper.mapLine("bar", 1));
	}

}
