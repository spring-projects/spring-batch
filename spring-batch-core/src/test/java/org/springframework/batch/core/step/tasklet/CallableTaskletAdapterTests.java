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
package org.springframework.batch.core.step.tasklet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.springframework.batch.repeat.RepeatStatus;

class CallableTaskletAdapterTests {

	private final CallableTaskletAdapter adapter = new CallableTaskletAdapter();

	@Test
	void testHandle() throws Exception {
		adapter.setCallable(new Callable<RepeatStatus>() {
			@Override
			public RepeatStatus call() throws Exception {
				return RepeatStatus.FINISHED;
			}
		});
		assertEquals(RepeatStatus.FINISHED, adapter.execute(null, null));
	}

	@Test
	void testAfterPropertiesSet() {
		assertThrows(IllegalArgumentException.class, adapter::afterPropertiesSet);
	}

}
