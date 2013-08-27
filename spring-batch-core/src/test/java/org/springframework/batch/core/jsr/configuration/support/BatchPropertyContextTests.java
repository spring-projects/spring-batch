/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * <p>
 * Test cases around {@link BatchPropertyContext}.
 * </p>
 *
 * @author Chris Schaefer
 */
public class BatchPropertyContextTests {
	private List<BatchPropertyContext.BatchPropertyContextEntry> entries = new ArrayList<BatchPropertyContext.BatchPropertyContextEntry>();

	@Before
	public void setUp() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();

		Properties step1Properties = new Properties();
		step1Properties.setProperty("step1PropertyName1", "step1PropertyValue1");
		step1Properties.setProperty("step1PropertyName2", "step1PropertyValue2");
		entries.add(batchPropertyContext.new BatchPropertyContextEntry("job1.step1", step1Properties));

		Properties step2Properties = new Properties();
		step2Properties.setProperty("step2PropertyName1", "step2PropertyValue1");
		step2Properties.setProperty("step2PropertyName2", "step2PropertyValue2");
		entries.add(batchPropertyContext.new BatchPropertyContextEntry("job1.step2", step2Properties));

		Properties jobProperties = new Properties();
		jobProperties.setProperty("jobProperty1", "jobProperty1value");
		jobProperties.setProperty("jobProperty2", "jobProperty2value");
		entries.add(batchPropertyContext.new BatchPropertyContextEntry("job1.job-job1", jobProperties));
	}

	@Test
	public void testAddBatchContextEntries() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();
		batchPropertyContext.setBatchContextEntries(entries);

		Properties step1BatchProperties = batchPropertyContext.getBatchProperties("job1.step1");
		assertEquals(4, step1BatchProperties.size());
		assertEquals("step1PropertyValue1", step1BatchProperties.getProperty("step1PropertyName1"));
		assertEquals("step1PropertyValue2", step1BatchProperties.getProperty("step1PropertyName2"));
		assertEquals("jobProperty1value", step1BatchProperties.getProperty("jobProperty1"));
		assertEquals("jobProperty2value", step1BatchProperties.getProperty("jobProperty2"));

		Properties step2BatchProperties = batchPropertyContext.getBatchProperties("job1.step2");
		assertEquals(4, step2BatchProperties.size());
		assertEquals("step2PropertyValue1", step2BatchProperties.getProperty("step2PropertyName1"));
		assertEquals("step2PropertyValue2", step2BatchProperties.getProperty("step2PropertyName2"));
		assertEquals("jobProperty1value", step2BatchProperties.getProperty("jobProperty1"));
		assertEquals("jobProperty2value", step2BatchProperties.getProperty("jobProperty2"));

		Properties jobProperties = batchPropertyContext.getBatchProperties("job1.job-job1");
		assertEquals(2, jobProperties.size());
		assertEquals("jobProperty1value", jobProperties.getProperty("jobProperty1"));
		assertEquals("jobProperty2value", jobProperties.getProperty("jobProperty2"));
	}

	@Test
	public void testAddBatchContextEntriesToExistingArtifact() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();

		Properties step1properties = new Properties();
		step1properties.setProperty("newStep1PropertyName", "newStep1PropertyValue");
		entries.add(batchPropertyContext.new BatchPropertyContextEntry("job1.step1", step1properties));

		batchPropertyContext.setBatchContextEntries(entries);

		Properties bean2 = batchPropertyContext.getBatchProperties("job1.step1");
		assertEquals(5, bean2.size());
		assertEquals("step1PropertyValue1", bean2.getProperty("step1PropertyName1"));
		assertEquals("step1PropertyValue2", bean2.getProperty("step1PropertyName2"));
		assertEquals("newStep1PropertyValue", bean2.getProperty("newStep1PropertyName"));
		assertEquals("jobProperty1value", bean2.getProperty("jobProperty1"));
		assertEquals("jobProperty2value", bean2.getProperty("jobProperty2"));
	}

	@Test
	public void testGetStepLevelProperties() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();
		batchPropertyContext.setBatchContextEntries(entries);

		Properties bean1 = batchPropertyContext.getStepLevelProperties("job1.step1");
		assertEquals(2, bean1.size());
		assertEquals("step1PropertyValue1", bean1.getProperty("step1PropertyName1"));
		assertEquals("step1PropertyValue2", bean1.getProperty("step1PropertyName2"));

		Properties bean2 = batchPropertyContext.getStepLevelProperties("job1.step2");
		assertEquals(2, bean2.size());
		assertEquals("step2PropertyValue1", bean2.getProperty("step2PropertyName1"));
		assertEquals("step2PropertyValue2", bean2.getProperty("step2PropertyName2"));
	}

	@Test
	public void testJobProperties() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();
		batchPropertyContext.setBatchContextEntries(entries);

		Properties jobProperties = batchPropertyContext.getJobProperties();
		assertEquals(2, jobProperties.size());
		assertEquals("jobProperty1value", jobProperties.getProperty("jobProperty1"));
		assertEquals("jobProperty2value", jobProperties.getProperty("jobProperty2"));
	}

	@Test
	public void testJobNonOverridingJobProperties() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();

		Properties jobProperties = new Properties();
		jobProperties.setProperty("step1PropertyName1", "step1PropertyOverride");
		entries.add(batchPropertyContext.new BatchPropertyContextEntry("job1.job-job1", jobProperties));

		batchPropertyContext.setBatchContextEntries(entries);

		Properties bean1 = batchPropertyContext.getBatchProperties("job1.step1");
		assertEquals(4, bean1.size());
		assertEquals("step1PropertyValue1", bean1.getProperty("step1PropertyName1"));
		assertEquals("step1PropertyValue2", bean1.getProperty("step1PropertyName2"));
		assertEquals("jobProperty1value", bean1.getProperty("jobProperty1"));
		assertEquals("jobProperty2value", bean1.getProperty("jobProperty2"));

		Properties testJobBean = batchPropertyContext.getBatchProperties("job1.job-job1");
		assertEquals(3, testJobBean.size());
		assertEquals("step1PropertyOverride", testJobBean.getProperty("step1PropertyName1"));
		assertEquals("jobProperty1value", testJobBean.getProperty("jobProperty1"));
		assertEquals("jobProperty2value", testJobBean.getProperty("jobProperty2"));
	}

	@Test
	public void testJobLevelPropertiesWithPath() {
		List<BatchPropertyContext.BatchPropertyContextEntry> entries = new ArrayList<BatchPropertyContext.BatchPropertyContextEntry>();

		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();

		Properties jobProperties = new Properties();
		jobProperties.setProperty("readerName", "testJobreaderName");

		entries.add(batchPropertyContext.new BatchPropertyContextEntry("job1.job-job1.itemReader", jobProperties));

		batchPropertyContext.setBatchContextEntries(entries);

		Properties props = batchPropertyContext.getJobProperties();
		assertEquals(1, props.size());
		assertEquals("testJobreaderName", props.getProperty("readerName"));
	}

	@Test
	public void testJobLevelComponentPath() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();
		assertTrue(batchPropertyContext.isJobLevelComponentPath("myJob.job-myJob"));
		assertTrue(batchPropertyContext.isJobLevelComponentPath("myJob.job-myJob.myReader"));
		assertTrue(batchPropertyContext.isJobLevelComponentPath("myJob.job-myJob.myReader.something"));
		assertFalse(batchPropertyContext.isJobLevelComponentPath("myJob"));
		assertFalse(batchPropertyContext.isJobLevelComponentPath("job-myJob"));
		assertFalse(batchPropertyContext.isJobLevelComponentPath(null));
		assertFalse(batchPropertyContext.isJobLevelComponentPath("myJob."));
	}
}
