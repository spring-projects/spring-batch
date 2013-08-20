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

		Properties bean1Properties = new Properties();
		bean1Properties.setProperty("readerName", "bean1readerName");
		bean1Properties.setProperty("readerWriter", "bean1writerName");
		entries.add(batchPropertyContext.new BatchPropertyContextEntry("bean1", bean1Properties));

		Properties bean2Properties = new Properties();
		bean2Properties.setProperty("readerName", "bean2readerName");
		bean2Properties.setProperty("readerWriter", "bean2writerName");
		entries.add(batchPropertyContext.new BatchPropertyContextEntry("bean2", bean2Properties));

		Properties jobProperties = new Properties();
		jobProperties.setProperty("jobProperty1", "jobProperty1value");
		jobProperties.setProperty("jobProperty2", "jobProperty2value");
		entries.add(batchPropertyContext.new BatchPropertyContextEntry("job-testJob", jobProperties));
	}

	@Test
	public void testAddBatchContextEntries() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();
		batchPropertyContext.setBatchContextEntries(entries);

		Properties bean1 = batchPropertyContext.getBatchProperties("bean1");
		assertEquals(4, bean1.size());
		assertEquals("bean1readerName", bean1.getProperty("readerName"));
		assertEquals("bean1writerName", bean1.getProperty("readerWriter"));
		assertEquals("jobProperty1value", bean1.getProperty("jobProperty1"));
		assertEquals("jobProperty2value", bean1.getProperty("jobProperty2"));

		Properties bean2 = batchPropertyContext.getBatchProperties("bean2");
		assertEquals(4, bean2.size());
		assertEquals("bean2readerName", bean2.getProperty("readerName"));
		assertEquals("bean2writerName", bean2.getProperty("readerWriter"));
		assertEquals("jobProperty1value", bean2.getProperty("jobProperty1"));
		assertEquals("jobProperty2value", bean2.getProperty("jobProperty2"));

		Properties jobProperties = batchPropertyContext.getBatchProperties("job-testJob");
		assertEquals(2, jobProperties.size());
		assertEquals("jobProperty1value", jobProperties.getProperty("jobProperty1"));
		assertEquals("jobProperty2value", jobProperties.getProperty("jobProperty2"));
	}

	@Test
	public void testAddBatchContextEntriesToExistingArtifact() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();

		Properties bean2Properties = new Properties();
		bean2Properties.setProperty("processorName", "bean2processorName");
		entries.add(batchPropertyContext.new BatchPropertyContextEntry("bean2", bean2Properties));

		batchPropertyContext.setBatchContextEntries(entries);

		Properties bean2 = batchPropertyContext.getBatchProperties("bean2");
		assertEquals(5, bean2.size());
		assertEquals("bean2readerName", bean2.getProperty("readerName"));
		assertEquals("bean2writerName", bean2.getProperty("readerWriter"));
		assertEquals("bean2processorName", bean2.getProperty("processorName"));
		assertEquals("jobProperty1value", bean2.getProperty("jobProperty1"));
		assertEquals("jobProperty2value", bean2.getProperty("jobProperty2"));
	}

	@Test
	public void testGetStepLevelProperties() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();
		batchPropertyContext.setBatchContextEntries(entries);

		Properties bean1 = batchPropertyContext.getStepLevelProperties("bean1");
		assertEquals(2, bean1.size());
		assertEquals("bean1readerName", bean1.getProperty("readerName"));
		assertEquals("bean1writerName", bean1.getProperty("readerWriter"));

		Properties bean2 = batchPropertyContext.getStepLevelProperties("bean2");
		assertEquals(2, bean2.size());
		assertEquals("bean2readerName", bean2.getProperty("readerName"));
		assertEquals("bean2writerName", bean2.getProperty("readerWriter"));
	}

	@Test
	public void testGetScopedStepLevelProperties() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();

		Properties scopedBeanProperties = new Properties();
		scopedBeanProperties.setProperty("scopedBeanName", "scopedBeanValue");
		entries.add(batchPropertyContext.new BatchPropertyContextEntry("scopedBean", scopedBeanProperties));

		batchPropertyContext.setBatchContextEntries(entries);

		Properties bean1 = batchPropertyContext.getStepLevelProperties("bean1");
		assertEquals(2, bean1.size());
		assertEquals("bean1readerName", bean1.getProperty("readerName"));
		assertEquals("bean1writerName", bean1.getProperty("readerWriter"));

		Properties bean2 = batchPropertyContext.getStepLevelProperties("bean2");
		assertEquals(2, bean2.size());
		assertEquals("bean2readerName", bean2.getProperty("readerName"));
		assertEquals("bean2writerName", bean2.getProperty("readerWriter"));

		Properties scopedBean = batchPropertyContext.getStepLevelProperties("scopedTarget.scopedBean");
		assertEquals(1, scopedBean.size());
		assertEquals("scopedBeanValue", scopedBean.getProperty("scopedBeanName"));
	}

	@Test
	public void testGetScopedBatchProperties() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();

		Properties scopedBeanProperties = new Properties();
		scopedBeanProperties.setProperty("scopedBeanName", "scopedBeanValue");
		entries.add(batchPropertyContext.new BatchPropertyContextEntry("scopedBean", scopedBeanProperties));

		batchPropertyContext.setBatchContextEntries(entries);

		Properties bean1 = batchPropertyContext.getBatchProperties("bean1");
		assertEquals(4, bean1.size());
		assertEquals("bean1readerName", bean1.getProperty("readerName"));
		assertEquals("bean1writerName", bean1.getProperty("readerWriter"));
		assertEquals("jobProperty1value", bean1.getProperty("jobProperty1"));
		assertEquals("jobProperty2value", bean1.getProperty("jobProperty2"));

		Properties bean2 = batchPropertyContext.getBatchProperties("bean2");
		assertEquals(4, bean2.size());
		assertEquals("bean2readerName", bean2.getProperty("readerName"));
		assertEquals("bean2writerName", bean2.getProperty("readerWriter"));
		assertEquals("jobProperty1value", bean2.getProperty("jobProperty1"));
		assertEquals("jobProperty2value", bean2.getProperty("jobProperty2"));

		Properties scopedBean = batchPropertyContext.getBatchProperties("scopedTarget.scopedBean");
		assertEquals(3, scopedBean.size());
		assertEquals("scopedBeanValue", scopedBean.getProperty("scopedBeanName"));
		assertEquals("jobProperty1value", scopedBean.getProperty("jobProperty1"));
		assertEquals("jobProperty2value", scopedBean.getProperty("jobProperty2"));
	}

	@Test
	public void testGetOriginalBeanName() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();
		String originalName = batchPropertyContext.getOriginalBeanName("scopedTarget.myBean");
		assertTrue(originalName.equals("myBean"));
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
		jobProperties.setProperty("readerName", "testJobreaderName");
		entries.add(batchPropertyContext.new BatchPropertyContextEntry("job-testJob", jobProperties));

		batchPropertyContext.setBatchContextEntries(entries);

		Properties bean1 = batchPropertyContext.getBatchProperties("bean1");
		assertEquals(4, bean1.size());
		assertEquals("bean1readerName", bean1.getProperty("readerName"));
		assertEquals("bean1writerName", bean1.getProperty("readerWriter"));
		assertEquals("jobProperty1value", bean1.getProperty("jobProperty1"));
		assertEquals("jobProperty2value", bean1.getProperty("jobProperty2"));

		Properties testJobBean = batchPropertyContext.getBatchProperties("job-testJob");
		assertEquals(3, testJobBean.size());
		assertEquals("testJobreaderName", testJobBean.getProperty("readerName"));
		assertEquals("jobProperty1value", testJobBean.getProperty("jobProperty1"));
		assertEquals("jobProperty2value", testJobBean.getProperty("jobProperty2"));
	}
}
