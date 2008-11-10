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

import static org.junit.Assert.fail;

import org.junit.Test;


/**
 * @author Dave Syer
 */
public class TaskExecutorRepeatTemplateTests extends SimpleRepeatTemplateTests {

	public RepeatTemplate getRepeatTemplate() {
		return new TaskExecutorRepeatTemplate();
	}
	
	@Test
	public void testSetThrottleLimit() throws Exception {
		try {
			new TaskExecutorRepeatTemplate().setThrottleLimit(-1);
		} catch (Exception e) {
			// unexpected - no check for illegal values
			fail("Unexpected Exception setting throttle limit");
		}
	}

}
