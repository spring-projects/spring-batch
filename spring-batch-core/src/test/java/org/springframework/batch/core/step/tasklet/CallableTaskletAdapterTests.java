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
package org.springframework.batch.core.step.tasklet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;

import org.junit.Test;
import org.springframework.batch.repeat.RepeatStatus;

public class CallableTaskletAdapterTests {
	
	private CallableTaskletAdapter adapter = new CallableTaskletAdapter();

	@Test
	public void testHandle() throws Exception {
		adapter.setCallable(new Callable<RepeatStatus>() {
			public RepeatStatus call() throws Exception {
				return RepeatStatus.FINISHED;
			}
		});
		assertEquals(RepeatStatus.FINISHED, adapter.execute(null,null));
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		try {
			adapter.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}
	
}
