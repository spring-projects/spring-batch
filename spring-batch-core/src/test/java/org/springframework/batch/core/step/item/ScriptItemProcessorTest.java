/*
 * Copyright 2014 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import java.util.List;

/**
 * <p>
 * Test job utilizing a {@link org.springframework.batch.item.support.ScriptItemProcessor}.
 * </p>
 *
 * @author Chris Schaefer
 * @since 3.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ScriptItemProcessorTest {
	@Autowired
	private Job job;

	@Autowired
	private JobLauncher jobLauncher;

	@Test
	public void testScriptProcessorJob() throws Exception {
		jobLauncher.run(job, new JobParameters());
	}

	public static class TestItemWriter implements ItemWriter<String> {
		@Override
		public void write(List<? extends String> items) throws Exception {
			Assert.notNull(items, "Items cannot be null");
			Assert.isTrue(!items.isEmpty(), "Items cannot be empty");
			Assert.isTrue(items.size() == 1, "Items should only contain one entry");

			String item = items.get(0);
			Assert.isTrue("BLAH".equals(item), "Transformed item to write should have been: BLAH but got: " + item);
		}
	}
}
