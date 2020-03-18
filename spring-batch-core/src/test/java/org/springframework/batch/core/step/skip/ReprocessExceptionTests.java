/*
 * Copyright 2014-2019 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author mminella
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ReprocessExceptionTests {

	@Autowired
	public Job job;

	@Autowired
	public JobLauncher jobLauncher;

	@Test
	public void testReprocessException() throws Exception {
		JobExecution execution = jobLauncher.run(job, new JobParametersBuilder().toJobParameters());

		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	public static class PersonProcessor implements ItemProcessor<Person, Person> {

		private String mostRecentFirstName;

		@Nullable
		@Override
		public Person process(final Person person) throws Exception {
			if (person.getFirstName().equals(mostRecentFirstName)) {
				throw new RuntimeException("throwing a exception during process after a rollback");
			}
			mostRecentFirstName = person.getFirstName();

			final String firstName = person.getFirstName().toUpperCase();
			final String lastName = person.getLastName().toUpperCase();

			final Person transformedPerson = new Person(firstName, lastName);

			System.out.println("Converting (" + person + ") into (" + transformedPerson + ")");

			return transformedPerson;
		}
	}

	public static class PersonItemWriter implements ItemWriter<Person> {
		@Override
		public void write(List<? extends Person> persons) throws Exception {
			for (Person person : persons) {
				System.out.println(person.getFirstName() + " " + person.getLastName());
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
