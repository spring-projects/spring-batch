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

package org.springframework.batch.repeat.callback;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;

/**
 * Simple wrapper for two business interfaces: get the next item from a
 * reader and apply the given writer to the result (if not null).
 * 
 * @author Dave Syer
 * 
 */
public class ItemReaderRepeatCallback implements RepeatCallback {

	ItemReader provider;

	ItemWriter writer;

	public ItemReaderRepeatCallback(ItemReader provider, ItemWriter writer) {
		super();
		this.provider = provider;
		this.writer = writer;
	}

	/**
	 * Default writer is null, in which case we do nothing - subclasses can
	 * extend this behaviour, but must be careful to actually exhaust the
	 * provider by calling next().
	 * @param provider
	 */
	public ItemReaderRepeatCallback(ItemReader provider) {
		this(provider, null);
	}

	/**
	 * Use the writer to process the next item if there is one. Return the
	 * item processed, or null if nothing was available.
	 * @see org.springframework.batch.repeat.RepeatCallback#doInIteration(RepeatContext)
	 * @param context the current context.
	 * @return null if the data provider is exhausted.
	 */
	public ExitStatus doInIteration(RepeatContext context) throws Exception {

		ExitStatus result = ExitStatus.FINISHED;
		Object item = provider.read();

		if (writer != null) {
			if (item != null) {
				writer.write(item);
				result = ExitStatus.CONTINUABLE;
			}
			item = null;
		}

		return result;
	}

}
