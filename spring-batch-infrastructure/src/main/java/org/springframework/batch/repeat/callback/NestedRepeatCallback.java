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

import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * Callback that delegates to another callback, via a {@link RepeatOperations}
 * instance. Useful when nesting or composing batches in one another, e.g. for
 * breaking a batch down into chunks.
 * 
 * @author Dave Syer
 * 
 */
public class NestedRepeatCallback implements RepeatCallback {

	private RepeatOperations template;

	private RepeatCallback callback;

	/**
	 * Constructor setting mandatory fields.
	 * 
	 * @param template the {@link RepeatOperations} to use when calling the
	 * delegate callback
	 * @param callback the {@link RepeatCallback} delegate
	 */
	public NestedRepeatCallback(RepeatOperations template, RepeatCallback callback) {
		super();
		this.template = template;
		this.callback = callback;
	}

	/**
	 * Simply calls template.execute(callback). Clients can use this to repeat a
	 * batch process, or to break a process up into smaller chunks (e.g. to
	 * change the transaction boundaries).
	 * 
	 * @see org.springframework.batch.repeat.RepeatCallback#doInIteration(RepeatContext)
	 */
	public RepeatStatus doInIteration(RepeatContext context) throws Exception {
		return template.iterate(callback);
	}
}
