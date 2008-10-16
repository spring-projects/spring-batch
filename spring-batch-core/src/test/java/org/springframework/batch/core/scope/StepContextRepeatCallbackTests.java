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
package org.springframework.batch.core.scope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatContext;

/**
 * @author Dave Syer
 * 
 */
public class StepContextRepeatCallbackTests {
	
	private StepExecution stepExecution = new StepExecution("foo", new JobExecution(0L), 123L);
	private boolean addedAttribute = false;
	private boolean removedAttribute = false;

	@Test
	public void testDoInIteration() throws Exception {
		StepContextRepeatCallback callback = new StepContextRepeatCallback(stepExecution) {
			@Override
			public ExitStatus doInStepContext(RepeatContext context, StepContext stepContext) throws Exception {
				assertEquals(Long.valueOf(123), stepContext.getStepExecution().getId());
				return ExitStatus.NOOP;
			}
		};
		assertEquals(ExitStatus.NOOP, callback.doInIteration(null));
	}

	@Test
	public void testUnfinishedWork() throws Exception {
		StepContextRepeatCallback callback = new StepContextRepeatCallback(stepExecution) {
			@Override
			public ExitStatus doInStepContext(RepeatContext context, StepContext stepContext) throws Exception {
				if (addedAttribute) {
					removedAttribute = stepContext.hasAttribute("foo");
					stepContext.removeAttribute("foo");
				} else {
					addedAttribute = true;
					stepContext.setAttribute("foo", "bar");
				}
				return ExitStatus.NOOP;
			}
		};
		callback.doInIteration(null);
		assertTrue(addedAttribute);
		callback.doInIteration(null);
		assertTrue(removedAttribute);
		callback.doInIteration(null);
		assertFalse(removedAttribute);
	}
}
