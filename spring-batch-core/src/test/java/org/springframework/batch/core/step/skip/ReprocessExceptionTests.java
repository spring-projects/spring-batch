/*
 * Copyright 2014-2025 the original author or authors.
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
package org.springframework.batch.core.step.skip;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author mminella
 * @author Mahmoud Ben Hassine
 */
@SpringJUnitConfig
public class ReprocessExceptionTests {

	@Autowired
	public Job job;

	@Autowired
	public JobOperator jobOperator;

	@Test
	void testReprocessException() throws Exception {
		JobExecution execution = jobOperator.start(job, new JobParametersBuilder().toJobParameters());

		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	public static class PersonProcessor implements ItemProcessor<Person, Person> {

		private String mostRecentFirstName;

		@Override
		public @Nullable Person process(Person person) throws Exception {
			if (person.getFirstName().equals(mostRecentFirstName)) {
				throw new RuntimeException("throwing a exception during process after a rollback");
			}
			mostRecentFirstName = person.getFirstName();

			final String firstName = person.getFirstName().toUpperCase();
			final String lastName = person.getLastName().toUpperCase();

			final Person transformedPerson = new Person(firstName, lastName);

			return transformedPerson;
		}

	}

	public static class PersonItemWriter implements ItemWriter<Person> {

		@Override
		public void write(Chunk<? extends Person> persons) throws Exception {
			for (Person person : persons) {
				if (person.getFirstName().equals("JANE")) {
					throw new RuntimeException("jane doe write exception causing rollback");
				}
			}
		}

	}

	public static class Person {

		private String lastName;

		private String firstName;

		public Person() {

		}

		public Person(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getFirstName() {
			return firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		@Override
		public String toString() {
			return "firstName: " + firstName + ", lastName: " + lastName;
		}

	}

}
