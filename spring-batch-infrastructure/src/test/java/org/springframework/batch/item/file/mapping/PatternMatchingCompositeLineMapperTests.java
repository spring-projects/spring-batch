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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.file.transform.Name;
import org.springframework.lang.Nullable;

/**
 * @author Dan Garrette
 * @author Dave Syer
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
		tokenizers.put("foo*", new LineTokenizer() {
			@Override
			public FieldSet tokenize(@Nullable String line) {
				return new DefaultFieldSet(new String[] { "a", "b" });
			}
		});
		tokenizers.put("bar*", new LineTokenizer() {
			@Override
			public FieldSet tokenize(@Nullable String line) {
				return new DefaultFieldSet(new String[] { "c", "d" });
			}
		});
		mapper.setTokenizers(tokenizers);

		Map<String, FieldSetMapper<Name>> fieldSetMappers = new HashMap<>();
		fieldSetMappers.put("foo*", new FieldSetMapper<Name>() {
			@Override
			public Name mapFieldSet(FieldSet fs) {
				return new Name(fs.readString(0), fs.readString(1), 0);
			}
		});
		fieldSetMappers.put("bar*", new FieldSetMapper<Name>() {
			@Override
			public Name mapFieldSet(FieldSet fs) {
				return new Name(fs.readString(1), fs.readString(0), 0);
			}
		});
		mapper.setFieldSetMappers(fieldSetMappers);

		Name name = mapper.mapLine("bar", 1);
		assertEquals(new Name("d", "c", 0), name);
	}

	@Test
	void testMapperKeyNotFound() {
		Map<String, LineTokenizer> tokenizers = new HashMap<>();
		tokenizers.put("foo*", new LineTokenizer() {
			@Override
			public FieldSet tokenize(@Nullable String line) {
				return new DefaultFieldSet(new String[] { "a", "b" });
			}
		});
		tokenizers.put("bar*", new LineTokenizer() {
			@Override
			public FieldSet tokenize(@Nullable String line) {
				return new DefaultFieldSet(new String[] { "c", "d" });
			}
		});
		mapper.setTokenizers(tokenizers);

		Map<String, FieldSetMapper<Name>> fieldSetMappers = new HashMap<>();
		fieldSetMappers.put("foo*", new FieldSetMapper<Name>() {
			@Override
			public Name mapFieldSet(FieldSet fs) {
				return new Name(fs.readString(0), fs.readString(1), 0);
			}
		});
		mapper.setFieldSetMappers(fieldSetMappers);

		assertThrows(IllegalStateException.class, () -> mapper.mapLine("bar", 1));
	}

}
