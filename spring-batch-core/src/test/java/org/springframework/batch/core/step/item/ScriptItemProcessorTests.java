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
package org.springframework.batch.core.step.item;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.infrastructure.item.support.ScriptItemProcessor;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.Assert;

/**
 * <p>
 * Test job utilizing a {@link ScriptItemProcessor}.
 * </p>
 *
 * @author Chris Schaefer
 * @author Mahmoud Ben Hassine
 * @since 3.1
 */
@SpringJUnitConfig
class ScriptItemProcessorTests {

	@Autowired
	private Job job;

	@Autowired
	private JobOperator jobOperator;

	@Test
	void testScriptProcessorJob() throws Exception {
		jobOperator.start(job, new JobParameters());
	}

	public static class TestItemWriter implements ItemWriter<String> {

		@Override
		public void write(Chunk<? extends String> chunk) throws Exception {
			Assert.notNull(chunk.getItems(), "Items cannot be null");
			Assert.isTrue(!chunk.getItems().isEmpty(), "Items cannot be empty");
			Assert.isTrue(chunk.getItems().size() == 1, "Items should only contain one entry");

			String item = chunk.getItems().get(0);
			Assert.isTrue("BLAH".equals(item), "Transformed item to write should have been: BLAH but got: " + item);
		}

	}

}
