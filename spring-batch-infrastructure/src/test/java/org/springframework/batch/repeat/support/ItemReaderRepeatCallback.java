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
package org.springframework.batch.repeat.support;

import java.util.Collections;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;

/**
 * @author Dave Syer
 *
 */
public class ItemReaderRepeatCallback<T> implements RepeatCallback {

	private final ItemReader<T> reader;
	private final ItemWriter<T> writer;

	/**
	 * @param reader
	 * @param writer
	 */
	public ItemReaderRepeatCallback(ItemReader<T> reader, ItemWriter<T> writer) {
		this.writer = writer;
		this.reader = reader;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.repeat.RepeatCallback#doInIteration(org.springframework.batch.repeat.RepeatContext)
	 */
	public RepeatStatus doInIteration(RepeatContext context) throws Exception {
		T item = reader.read();
		if (item==null) {
			return RepeatStatus.FINISHED;
		}
		writer.write(Collections.singletonList(item));
		return RepeatStatus.CONTINUABLE;
	}

}
