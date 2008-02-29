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
package org.springframework.batch.sample.item.writer;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.sample.tasklet.JobSupport;
import org.springframework.batch.sample.tasklet.StepSupport;
import org.springframework.test.AbstractTransactionalDataSourceSpringContextTests;
import org.springframework.util.ClassUtils;

public class StagingItemWriterTests extends AbstractTransactionalDataSourceSpringContextTests {

	private StagingItemWriter writer;

	public void setWriter(StagingItemWriter processor) {
		this.writer = processor;
	}

	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.addResourcePathToPackagePath(StagingItemWriter.class,
				"staging-test-context.xml") };
	}

	/* (non-Javadoc)
	 * @see org.springframework.test.AbstractTransactionalSpringContextTests#onSetUpBeforeTransaction()
	 */
	protected void onSetUpBeforeTransaction() throws Exception {
		StepExecution stepExecution = new StepExecution(new StepSupport("stepName"),
				new JobExecution(new JobInstance(new Long(12L), new JobParameters(), new JobSupport("testJob"))));
		writer.beforeStep(stepExecution);
	}

	public void testProcessInsertsNewItem() throws Exception {
		int before = getJdbcTemplate().queryForInt("SELECT COUNT(*) from BATCH_STAGING");
		writer.write("FOO");
		int after = getJdbcTemplate().queryForInt("SELECT COUNT(*) from BATCH_STAGING");
		assertEquals(before + 1, after);
	}

}
