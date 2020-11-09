/*
 * Copyright 2006-2020 the original author or authors.
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
package org.springframework.batch.core.launch.support;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 */
public class RunIdIncrementerTests {

	private RunIdIncrementer incrementer = new RunIdIncrementer();

	@Test
	public void testGetNext() {
		JobParameters next = incrementer.getNext(null);
		assertEquals(1, next.getLong("run.id").intValue());
		assertEquals(2, incrementer.getNext(next).getLong("run.id").intValue());
	}

	@Test
	public void testGetNextAppends() {
		JobParameters next = incrementer.getNext(new JobParametersBuilder().addString("foo", "bar").toJobParameters());
		assertEquals(1, next.getLong("run.id").intValue());
		assertEquals("bar", next.getString("foo"));
	}

	@Test
	public void testGetNextNamed() {
		incrementer.setKey("foo");
		JobParameters next = incrementer.getNext(null);
		assertEquals(1, next.getLong("foo").intValue());
	}

	@Test
	public void testGetNextWhenRunIdIsString() {
		// given
		JobParameters parameters = new JobParametersBuilder()
				.addString("run.id", "5")
				.toJobParameters();

		// when
		JobParameters next = this.incrementer.getNext(parameters);

		// then
		assertEquals(Long.valueOf(6), next.getLong("run.id"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetNextWhenRunIdIsInvalidString() {
		this.incrementer.getNext(new JobParametersBuilder()
				.addString("run.id", "foo")
				.toJobParameters());
	}

}
