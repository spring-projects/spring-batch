/*
 * Copyright 2008-2013 the original author or authors.
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.LineTokenizer;

/**
 * Tests for {@link DefaultLineMapper}.
 */
public class DefaultLineMapperTests {

	private DefaultLineMapper<String> tested = new DefaultLineMapper<>();

	@Test(expected=IllegalArgumentException.class)
	public void testMandatoryTokenizer() throws Exception {
		tested.afterPropertiesSet();
		tested.mapLine("foo", 1);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testMandatoryMapper() throws Exception {
		tested.setLineTokenizer(new DelimitedLineTokenizer());
		tested.afterPropertiesSet();
		tested.mapLine("foo", 1);
	}
	
	@Test
	public void testMapping() throws Exception {
		final String line = "TEST";
		final FieldSet fs = new DefaultFieldSet(new String[]{"token1", "token2"});
		final String item = "ITEM";
		
		LineTokenizer tokenizer = mock(LineTokenizer.class);
		when(tokenizer.tokenize(line)).thenReturn(fs);
		
		@SuppressWarnings("unchecked")
		FieldSetMapper<String> fsMapper = mock(FieldSetMapper.class);
		when(fsMapper.mapFieldSet(fs)).thenReturn(item);
		
		tested.setLineTokenizer(tokenizer);
		tested.setFieldSetMapper(fsMapper);
		
		assertSame(item, tested.mapLine(line, 1));
		
	}
	
}
