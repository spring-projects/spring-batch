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
package org.springframework.batch.core.launch.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 */
class RunIdIncrementerTests {

	private final RunIdIncrementer incrementer = new RunIdIncrementer();

	@Test
	void testGetNext() {
		JobParameters next = incrementer.getNext(new JobParameters());
		assertEquals(1, next.getLong("run.id"));
		JobParameters nextNext = incrementer.getNext(next);
		assertEquals(2, nextNext.getLong("run.id"));
	}

	@Test
	void testGetNextAppends() {
		JobParameters jobParameters = new JobParametersBuilder().addString("foo", "bar").toJobParameters();
		JobParameters next = incrementer.getNext(jobParameters);
		assertEquals(1, next.getLong("run.id"));
		assertEquals("bar", next.getString("foo"));
	}

	@Test
	void testGetNextNamed() {
		incrementer.setKey("foo");
		JobParameters next = incrementer.getNext(new JobParameters());
		assertEquals(1, next.getLong("foo"));
	}

	@Test
	void testGetNextWhenRunIdIsString() {
		// given
		JobParameters parameters = new JobParametersBuilder().addString("run.id", "5").toJobParameters();

		// when
		JobParameters next = this.incrementer.getNext(parameters);

		// then
		assertEquals(Long.valueOf(6), next.getLong("run.id"));
	}

	@Test
	void testGetNextWhenRunIdIsInvalidString() {
		JobParameters jobParameters = new JobParametersBuilder().addString("run.id", "foo").toJobParameters();
		assertThrows(IllegalArgumentException.class, () -> this.incrementer.getNext(jobParameters));
	}

}
