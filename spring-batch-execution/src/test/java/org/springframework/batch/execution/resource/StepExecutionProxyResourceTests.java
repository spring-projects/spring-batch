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

package org.springframework.batch.execution.resource;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.JobParametersBuilder;
import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.execution.job.JobSupport;
import org.springframework.batch.execution.step.StepSupport;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * Unit tests for {@link StepExecutionProxyResource}
 * 
 * @author robert.kasanicky
 * @author Lucas Ward
 * @author Dave Syer
 */
public class StepExecutionProxyResourceTests extends TestCase {

	/**
	 * Object under test
	 */
	private StepExecutionProxyResource resource = new StepExecutionProxyResource();

	private char pathsep = File.separatorChar;

	private String path = "data" + pathsep;

	private JobInstance jobInstance;

	private StepExecution stepExecution;

	/**
	 * mock step context
	 */

	protected void setUp() throws Exception {

		jobInstance = new JobInstance(new Long(0), new JobParameters(), new JobSupport("testJob"));
		JobExecution jobExecution = new JobExecution(jobInstance);
		Step step = new StepSupport("bar");
		stepExecution = jobExecution.createStepExecution(step);
		resource.beforeStep(stepExecution);

	}

	/**
	 * regular use with valid context and pattern provided
	 */
	public void testCreateFileName() throws Exception {
		doTestPathName("bar.txt", path);
	}

	public void testNullFilePattern() throws Exception {
		resource.setFilePattern(null);
		try {
			resource.beforeStep(stepExecution);
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testNonStandardFilePattern() throws Exception {
		resource.setFilePattern("foo/data/%JOB_NAME%/" + "%STEP_NAME%-job");
		resource.beforeStep(stepExecution);
		doTestPathName("bar-job", "foo" + pathsep + "data" + pathsep);
	}

	public void testNonStandardFilePatternWithJobParameters() throws Exception {
		resource.setFilePattern("foo/data/%JOB_NAME%/%job.key%-foo");
		jobInstance = new JobInstance(new Long(0), new JobParametersBuilder().addString("job.key", "spam")
				.toJobParameters(), new JobSupport("testJob"));
		JobExecution jobExecution = new JobExecution(jobInstance);
		Step step = new StepSupport("bar");
		resource.beforeStep(jobExecution.createStepExecution(step));
		doTestPathName("spam-foo", "foo" + pathsep + "data" + pathsep);
	}

	public void testResoureLoaderAware() throws Exception {
		resource = new StepExecutionProxyResource();
		resource.setResourceLoader(new DefaultResourceLoader() {
			public Resource getResource(String location) {
				return new ByteArrayResource("foo".getBytes());
			}
		});
		resource.beforeStep(stepExecution);
		assertTrue(resource.exists());
	}

	private void doTestPathName(String filename, String path) throws Exception, IOException {
		String returnedPath = resource.getFile().getAbsolutePath();
		String absolutePath = new File(path + jobInstance.getJobName() + pathsep + filename).getAbsolutePath();
		assertEquals(absolutePath, returnedPath);
	}

}
