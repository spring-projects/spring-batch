/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.support;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

/**
 * <p>
 * Test cases around {@link BatchPropertyContext}.
 * </p>
 *
 * @author Chris Schaefer
 */
public class BatchPropertyContextTests {
	private Properties jobProperties = new Properties();
	private Map<String, Properties> stepProperties = new HashMap<>();
	private Map<String, Properties> artifactProperties = new HashMap<>();
	private Map<String, Map<String, Properties>> partitionProperties = new HashMap<>();
	private Map<String, Map<String, Properties>> stepArtifactProperties = new HashMap<>();

	@SuppressWarnings("serial")
	@Before
	public void setUp() {
		Properties step1Properties = new Properties();
		step1Properties.setProperty("step1PropertyName1", "step1PropertyValue1");
		step1Properties.setProperty("step1PropertyName2", "step1PropertyValue2");
		this.stepProperties.put("step1", step1Properties);

		Properties step2Properties = new Properties();
		step2Properties.setProperty("step2PropertyName1", "step2PropertyValue1");
		step2Properties.setProperty("step2PropertyName2", "step2PropertyValue2");
		this.stepProperties.put("step2", step2Properties);

		Properties jobProperties = new Properties();
		jobProperties.setProperty("jobProperty1", "jobProperty1value");
		jobProperties.setProperty("jobProperty2", "jobProperty2value");
		this.jobProperties.putAll(jobProperties);

		Properties artifactProperties = new Properties();
		artifactProperties.setProperty("deciderProperty1", "deciderProperty1value");
		artifactProperties.setProperty("deciderProperty2", "deciderProperty2value");
		this.artifactProperties.put("decider1", artifactProperties);

		final Properties stepArtifactProperties = new Properties();
		stepArtifactProperties.setProperty("readerProperty1", "readerProperty1value");
		stepArtifactProperties.setProperty("readerProperty2", "readerProperty2value");

		this.stepArtifactProperties.put("step1", new HashMap<String, Properties>() {{
			put("reader", stepArtifactProperties);
		}});

		final Properties partitionProperties = new Properties();
		partitionProperties.setProperty("writerProperty1", "writerProperty1valuePartition0");
		partitionProperties.setProperty("writerProperty2", "writerProperty2valuePartition0");

		this.partitionProperties.put("step2:partition0", new HashMap<String, Properties>() {{
			put("writer", partitionProperties);
		}});

		final Properties partitionStepProperties = new Properties();
		partitionStepProperties.setProperty("writerProperty1Step", "writerProperty1");
		partitionStepProperties.setProperty("writerProperty2Step", "writerProperty2");

		this.partitionProperties.put("step2", new HashMap<String, Properties>() {{
			put("writer", partitionStepProperties);
		}});
	}

	@Test
	public void testStepLevelProperties() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();
		batchPropertyContext.setJobProperties(jobProperties);
		batchPropertyContext.setStepProperties(stepProperties);

		Properties step1Properties = batchPropertyContext.getStepProperties("step1");
		assertEquals(2, step1Properties.size());
		assertEquals("step1PropertyValue1", step1Properties.getProperty("step1PropertyName1"));
		assertEquals("step1PropertyValue2", step1Properties.getProperty("step1PropertyName2"));

		Properties step2Properties = batchPropertyContext.getStepProperties("step2");
		assertEquals(2, step2Properties.size());
		assertEquals("step2PropertyValue1", step2Properties.getProperty("step2PropertyName1"));
		assertEquals("step2PropertyValue2", step2Properties.getProperty("step2PropertyName2"));
	}

	@Test
	public void testJobLevelProperties() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();
		batchPropertyContext.setJobProperties(jobProperties);

		Properties jobProperties = batchPropertyContext.getJobProperties();
		assertEquals(2, jobProperties.size());
		assertEquals("jobProperty1value", jobProperties.getProperty("jobProperty1"));
		assertEquals("jobProperty2value", jobProperties.getProperty("jobProperty2"));
	}

	@Test
	public void testAddPropertiesToExistingStep() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();
		batchPropertyContext.setJobProperties(jobProperties);
		batchPropertyContext.setStepProperties(stepProperties);

		Properties step1 = batchPropertyContext.getStepProperties("step1");
		assertEquals(2, step1.size());
		assertEquals("step1PropertyValue1", step1.getProperty("step1PropertyName1"));
		assertEquals("step1PropertyValue2", step1.getProperty("step1PropertyName2"));

		Properties step1properties = new Properties();
		step1properties.setProperty("newStep1PropertyName", "newStep1PropertyValue");

		batchPropertyContext.setStepProperties("step1", step1properties);

		Properties step1updated = batchPropertyContext.getStepProperties("step1");
		assertEquals(3, step1updated.size());
		assertEquals("step1PropertyValue1", step1updated.getProperty("step1PropertyName1"));
		assertEquals("step1PropertyValue2", step1updated.getProperty("step1PropertyName2"));
		assertEquals("newStep1PropertyValue", step1updated.getProperty("newStep1PropertyName"));
	}

	@Test
	public void testNonStepLevelArtifactProperties() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();
		batchPropertyContext.setJobProperties(jobProperties);
		batchPropertyContext.setArtifactProperties(artifactProperties);
		batchPropertyContext.setStepProperties(stepProperties);

		Properties artifactProperties = batchPropertyContext.getArtifactProperties("decider1");
		assertEquals(2, artifactProperties.size());
		assertEquals("deciderProperty1value", artifactProperties.getProperty("deciderProperty1"));
		assertEquals("deciderProperty2value", artifactProperties.getProperty("deciderProperty2"));
	}

	@Test
	public void testStepLevelArtifactProperties() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();
		batchPropertyContext.setJobProperties(jobProperties);
		batchPropertyContext.setArtifactProperties(artifactProperties);
		batchPropertyContext.setStepProperties(stepProperties);
		batchPropertyContext.setStepArtifactProperties(stepArtifactProperties);

		Properties artifactProperties = batchPropertyContext.getStepArtifactProperties("step1", "reader");
		assertEquals(4, artifactProperties.size());
		assertEquals("readerProperty1value", artifactProperties.getProperty("readerProperty1"));
		assertEquals("readerProperty2value", artifactProperties.getProperty("readerProperty2"));
		assertEquals("step1PropertyValue1", artifactProperties.getProperty("step1PropertyName1"));
		assertEquals("step1PropertyValue2", artifactProperties.getProperty("step1PropertyName2"));
	}

	@Test
	public void testArtifactNonOverridingJobProperties() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();
		batchPropertyContext.setJobProperties(jobProperties);
		batchPropertyContext.setArtifactProperties(artifactProperties);

		Properties jobProperties = new Properties();
		jobProperties.setProperty("deciderProperty1", "decider1PropertyOverride");

		batchPropertyContext.setJobProperties(jobProperties);

		Properties step1 = batchPropertyContext.getArtifactProperties("decider1");
		assertEquals(2, step1.size());
		assertEquals("deciderProperty1value", step1.getProperty("deciderProperty1"));
		assertEquals("deciderProperty2value", step1.getProperty("deciderProperty2"));

		Properties job = batchPropertyContext.getJobProperties();
		assertEquals(3, job.size());
		assertEquals("decider1PropertyOverride", job.getProperty("deciderProperty1"));
		assertEquals("jobProperty1value", job.getProperty("jobProperty1"));
		assertEquals("jobProperty2value", job.getProperty("jobProperty2"));
	}

	@Test
	public void testPartitionProperties() {
		BatchPropertyContext batchPropertyContext = new BatchPropertyContext();
		batchPropertyContext.setJobProperties(jobProperties);
		batchPropertyContext.setArtifactProperties(artifactProperties);
		batchPropertyContext.setStepProperties(stepProperties);
		batchPropertyContext.setStepArtifactProperties(stepArtifactProperties);
		batchPropertyContext.setStepArtifactProperties(partitionProperties);

		Properties artifactProperties = batchPropertyContext.getStepArtifactProperties("step2:partition0", "writer");
		assertEquals(6, artifactProperties.size());
		assertEquals("writerProperty1", artifactProperties.getProperty("writerProperty1Step"));
		assertEquals("writerProperty2", artifactProperties.getProperty("writerProperty2Step"));
		assertEquals("writerProperty1valuePartition0", artifactProperties.getProperty("writerProperty1"));
		assertEquals("writerProperty2valuePartition0", artifactProperties.getProperty("writerProperty2"));
		assertEquals("step2PropertyValue1", artifactProperties.getProperty("step2PropertyName1"));
		assertEquals("step2PropertyValue2", artifactProperties.getProperty("step2PropertyName2"));
	}
}
