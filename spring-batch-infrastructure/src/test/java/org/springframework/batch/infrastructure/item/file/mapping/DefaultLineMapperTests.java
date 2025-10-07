/*
 * Copyright 2008-2022 the original author or authors.
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
package org.springframework.batch.infrastructure.item.file.mapping;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.infrastructure.item.file.mapping.FieldSetMapper;
import org.springframework.batch.infrastructure.item.file.transform.DefaultFieldSet;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;
import org.springframework.batch.infrastructure.item.file.transform.LineTokenizer;

/**
 * Tests for {@link DefaultLineMapper}.
 */
class DefaultLineMapperTests {

	private final DefaultLineMapper<String> tested = new DefaultLineMapper<>();

	@Test
	void testMandatoryTokenizer() {
		assertThrows(IllegalStateException.class, tested::afterPropertiesSet);
	}

	@Test
	void testMandatoryMapper() {
		tested.setLineTokenizer(new DelimitedLineTokenizer());
		assertThrows(IllegalStateException.class, tested::afterPropertiesSet);
	}

	@Test
	void testMapping() throws Exception {
		final String line = "TEST";
		final FieldSet fs = new DefaultFieldSet(new String[] { "token1", "token2" });
		final String item = "ITEM";

		LineTokenizer tokenizer = mock();
		when(tokenizer.tokenize(line)).thenReturn(fs);

		@SuppressWarnings("unchecked")
		FieldSetMapper<String> fsMapper = mock();
		when(fsMapper.mapFieldSet(fs)).thenReturn(item);

		tested.setLineTokenizer(tokenizer);
		tested.setFieldSetMapper(fsMapper);

		assertSame(item, tested.mapLine(line, 1));

	}

}
