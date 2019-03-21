/*
 * Copyright 2008-2012 the original author or authors.
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
package org.springframework.batch.item.file;

import org.springframework.batch.item.AbstractItemStreamItemReaderTests;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * Tests for {@link FlatFileItemReader}.
 */
public class FlatFileItemReaderCommonTests extends AbstractItemStreamItemReaderTests{

	private static final String FOOS = "1 \n 2 \n 3 \n 4 \n 5 \n"; 
	
	@Override
	protected ItemReader<Foo> getItemReader() throws Exception {
		FlatFileItemReader<Foo> tested = new FlatFileItemReader<>();
		Resource resource = new ByteArrayResource(FOOS.getBytes());
		tested.setResource(resource);
		tested.setLineMapper(new LineMapper<Foo>() {
            @Override
			public Foo mapLine(String line, int lineNumber) {
				Foo foo = new Foo();
				foo.setValue(Integer.valueOf(line.trim()));
				return foo;
			}
		});
		
		tested.setSaveState(true);
		tested.afterPropertiesSet();
		return tested;
	}

	@Override
	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		FlatFileItemReader<Foo> reader = (FlatFileItemReader<Foo>) tested;
		reader.close();
		
		reader.setResource(new ByteArrayResource("".getBytes()));
		reader.afterPropertiesSet();
		
		reader.open(new ExecutionContext());
		
	}

	
	
}
