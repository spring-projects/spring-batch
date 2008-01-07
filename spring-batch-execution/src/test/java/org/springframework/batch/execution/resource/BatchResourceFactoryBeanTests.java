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
import java.text.SimpleDateFormat;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.execution.resource.BatchResourceFactoryBean;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifier;
import org.springframework.batch.execution.scope.SimpleStepContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * Unit tests for {@link BatchResourceFactoryBean}
 * 
 * @author robert.kasanicky
 * @author Lucas Ward
 * @author Dave Syer
 */
public class BatchResourceFactoryBeanTests extends TestCase {

	/**
	 * Object under test
	 */
	private BatchResourceFactoryBean resourceFactory = new BatchResourceFactoryBean();

	private String rootDir = getRootDir();
	
	private char pathsep = File.separatorChar;

	private String path = "data" + pathsep;

	private ScheduledJobIdentifier identifier;

	/**
	 * mock step context
	 */

	protected void setUp() throws Exception {

		resourceFactory.setRootDirectory(rootDir);

		identifier = new ScheduledJobIdentifier("testJob", "testStream", new SimpleDateFormat("yyyyMMdd")
			.parse("20070730"));
		
		SimpleStepContext context = new SimpleStepContext();
		JobInstance job = new JobInstance(identifier);
		JobExecution jobExecution = new JobExecution(job);
		StepInstance step = new StepInstance(job, "bar");
		StepExecution stepExecution = new StepExecution(step, jobExecution);
		context.setStepExecution(stepExecution);
		resourceFactory.setStepContext(context);

		resourceFactory.afterPropertiesSet();
		
	}

	private String getRootDir() {
		String rootDir = System.getProperty("java.io.tmpdir");
		assertNotNull(rootDir);
		if (rootDir != null && rootDir.endsWith(File.separator)) {
			rootDir = rootDir.substring(0, rootDir.lastIndexOf(File.separator));
		}
		return rootDir;
	}

	/**
	 * regular use with valid context and pattern provided
	 */
	public void testCreateFileName() throws Exception {
		doTestPathName("testJob-testStream-20070730-bar.txt", path);
	}

	/**
	 * regular use with valid context and pattern provided
	 */
	public void testSetLabelGenerator() throws Exception {
		resourceFactory.setJobIdentifierLabelGenerator(new JobIdentifierLabelGenerator() {
			public String getLabel(JobIdentifier jobIdentifier) {
				return "foo";
			}
		});
		doTestPathName("foo-bar.txt", path);
	}

	public void testObjectType() throws Exception {
		assertEquals(Resource.class, resourceFactory.getObjectType());
	}

	public void testNullFilePattern() throws Exception {
		resourceFactory = new BatchResourceFactoryBean();
		resourceFactory.setFilePattern(null);
		try {
			resourceFactory.getObject();
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testNonStandardFilePattern() throws Exception {
		resourceFactory.setFilePattern("/%BATCH_ROOT%/data/%JOB_NAME%/"
				+ "%STEP_NAME%+%JOB_IDENTIFIER%");
		doTestPathName("bar+testJob-testStream-20070730", path);
	}

	public void testResoureLoaderAware() throws Exception {
		resourceFactory = new BatchResourceFactoryBean();
		resourceFactory.setSingleton(false);
		resourceFactory.setResourceLoader(new DefaultResourceLoader() {
			public Resource getResource(String location) {
				return new ByteArrayResource("foo".getBytes());
			}
		});
		Resource resource = (Resource) resourceFactory.getObject();
		assertNotNull(resource);
		assertTrue(resource.exists());
	}

	public void testRootDirectoryEndsWithForwardSlash() throws Exception {
		String rootDir = getRootDir();
		rootDir = StringUtils.replace(rootDir, File.separator, "/") + "/";
		resourceFactory.setRootDirectory(rootDir);
		doTestPathName("testJob-testStream-20070730-bar.txt", path);
	}

	public void testRootDirectoryEndsWithBackSlash() throws Exception {
		String rootDir = getRootDir();
		rootDir = "/"+StringUtils.replace(rootDir, File.separator, "\\") + "\\";
		resourceFactory.setRootDirectory(rootDir);
		doTestPathName("testJob-testStream-20070730-bar.txt", path);
	}

	private void doTestPathName(String filename, String path) throws Exception, IOException {
		Resource resource = (Resource) resourceFactory.getObject();
		
		String returnedPath = resource.getFile().getAbsolutePath();
		
		String absolutePath = new File("/" + rootDir + pathsep + path + identifier.getName() + pathsep + filename).getAbsolutePath();
		
		// System.err.println(absolutePath);
		// System.err.println(returnedPath);
		assertEquals(absolutePath, returnedPath);
	}
	
}
