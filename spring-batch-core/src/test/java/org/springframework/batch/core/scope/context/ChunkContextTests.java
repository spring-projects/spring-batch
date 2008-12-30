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
package org.springframework.batch.core.scope.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;

/**
 * @author Dave Syer
 * 
 */
public class ChunkContextTests {

	private ChunkContext context = new ChunkContext(new StepContext(new JobExecution(new JobInstance(0L,
			new JobParameters(Collections.singletonMap("foo", new JobParameter("bar"))), "job"), 1L)
			.createStepExecution("foo")));

	@Test
	public void testGetStepContext() {
		StepContext stepContext = context.getStepContext();
		assertNotNull(stepContext);
		assertEquals("bar", context.getStepContext().getJobParameters().get("foo"));
	}

	@Test
	public void testIsComplete() {
		assertFalse(context.isComplete());
		context.setComplete();
		assertTrue(context.isComplete());		
	}

	@Test
	public void testToString() {
		String value = context.toString();
		assertTrue("Wrong toString: "+value, value.contains("stepContext="));
		assertTrue("Wrong toString: "+value, value.contains("complete=false"));
		assertTrue("Wrong toString: "+value, value.contains("attributes=[]"));
	}

}
