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

import junit.framework.TestCase;

import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.support.RepeatTemplate;

public class NestedRepeatCallbackTests extends TestCase {

	int count = 0;

	public void testExecute() throws Exception {
		NestedRepeatCallback callback = new NestedRepeatCallback(new RepeatTemplate(), new RepeatCallback() {
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				count++;
				return RepeatStatus.continueIf(count <= 1);
			}
		});
		RepeatStatus result = callback.doInIteration(null);
		assertEquals(2, count);
		assertFalse(result.isContinuable()); // False because processing has finished
	}
}
