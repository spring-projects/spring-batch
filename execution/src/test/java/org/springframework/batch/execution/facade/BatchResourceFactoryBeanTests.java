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

package org.springframework.batch.execution.facade;

import java.io.File;
import java.util.Calendar;

import junit.framework.TestCase;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * Unit tests for {@link BatchResourceFactoryBean}
 * 
 * @author robert.kasanicky
 * @author Lucas Ward
 * @author Dave Syer
 */
public class BatchResourceFactoryBeanTests extends TestCase {

	/**
	 * object under test
	 */
	private BatchResourceFactoryBean resourceFactory = new BatchResourceFactoryBean();

	private String rootDir = getRootDir();
	
	private char pathsep = File.separatorChar;
	
	private String PATTERN_STRING = "%BATCH_ROOT%"+pathsep+"%JOB_NAME%-%SCHEDULE_DATE%-%JOB_RUN%-%STREAM_NAME%";

	private String EXPECTED_ABSOLUTE_PATH = rootDir+pathsep+"testJob-20070730-0-testStream";

	private String NULL_JOB_NAME_PATH = rootDir+pathsep+"%JOB_NAME%-20070730-0-testStream";

	/**
	 * mock step context
	 */

	protected void setUp() throws Exception {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, 2007);
		calendar.set(Calendar.MONTH, Calendar.JULY);
		calendar.set(Calendar.DAY_OF_MONTH, 30);

		// define mock behaviour
		resourceFactory.setScheduleDate("20070730");
		resourceFactory.setRootDirectory(rootDir);
		resourceFactory.setJobName("testJob");
		resourceFactory.setJobStream("testStream");
		resourceFactory.setJobRun(0);
		resourceFactory.setStepName("testStep");
		resourceFactory.setFilePattern(PATTERN_STRING);

		resourceFactory.afterPropertiesSet();
	}

	private String getRootDir() {
		String rootDir = System.getProperty("java.io.tmpdir");
		assertNotNull(rootDir);
		if (rootDir!=null && rootDir.endsWith(File.separator)) {
			rootDir = rootDir.substring(0, rootDir.lastIndexOf(File.separator));
		}
		return rootDir;
	}

	/**
	 * regular use with valid context and pattern provided
	 */
	public void testCreateFileName() throws Exception {

		Resource resource = (Resource) resourceFactory.getObject();

		String returnedPath = resource.getFile().getAbsolutePath();

		System.err.println(EXPECTED_ABSOLUTE_PATH);
		System.err.println(returnedPath);
		assertEquals(EXPECTED_ABSOLUTE_PATH, returnedPath);
	}

	/**
	 * Set the job name to null and attempt to get the resource, %JOB_NAME%
	 * should not be replaced.
	 */
	public void testNullJobName() throws Exception {

		resourceFactory.setJobName(null);
		// set singleton to false so a new instance is returned.
		resourceFactory.setSingleton(false);

		Resource resource = (Resource) resourceFactory.getObject();

		assertEquals(NULL_JOB_NAME_PATH, resource.getFile().getAbsolutePath());
	}
	
	public void testObjectType() throws Exception {
		assertEquals(Resource.class, resourceFactory.getObjectType());
	}
	
	public void testNullFilePattern() throws Exception {
		resourceFactory = new BatchResourceFactoryBean();
		resourceFactory.setSingleton(false);
		resourceFactory.setFilePattern(null);
		try {
			resourceFactory.getObject();
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// expected
		}
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

}
