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

package org.springframework.batch.infrastructure.repeat.callback;

import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.repeat.callback.NestedRepeatCallback;
import org.springframework.batch.infrastructure.repeat.support.RepeatTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NestedRepeatCallbackTests {

	private int count = 0;

	@Test
	void testExecute() throws Exception {
		NestedRepeatCallback callback = new NestedRepeatCallback(new RepeatTemplate(), context -> {
			count++;
			return RepeatStatus.continueIf(count <= 1);
		});
		RepeatStatus result = callback.doInIteration(null);
		assertEquals(2, count);
		assertFalse(result.isContinuable()); // False because processing has finished
	}

}
