/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.step.item.FaultTolerantChunkProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Dave Syer
 * 
 */
class ParentStepFactoryBeanParserTests {

	@Test
	void testSimpleAttributes() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/ParentStepFactoryBeanParserTests-context.xml");
		Object step = context.getBean("s1", Step.class);
		assertNotNull(step, "Step not parsed");
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		Object chunkProcessor = ReflectionTestUtils.getField(tasklet, "chunkProcessor");
		assertTrue(chunkProcessor instanceof FaultTolerantChunkProcessor<?, ?>, "Wrong processor type");
	}

	@Test
	void testSkippableAttributes() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/ParentSkippableStepFactoryBeanParserTests-context.xml");
		Object step = context.getBean("s1", Step.class);
		assertNotNull(step, "Step not parsed");
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		Object chunkProcessor = ReflectionTestUtils.getField(tasklet, "chunkProcessor");
		assertTrue(chunkProcessor instanceof FaultTolerantChunkProcessor<?, ?>, "Wrong processor type");
	}

	@Test
	void testRetryableAttributes() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/ParentRetryableStepFactoryBeanParserTests-context.xml");
		Object step = context.getBean("s1", Step.class);
		assertNotNull(step, "Step not parsed");
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		Object chunkProcessor = ReflectionTestUtils.getField(tasklet, "chunkProcessor");
		assertTrue(chunkProcessor instanceof FaultTolerantChunkProcessor<?, ?>, "Wrong processor type");
	}

	// BATCH-1396
	@Test
	void testRetryableLateBindingAttributes() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/ParentRetryableLateBindingStepFactoryBeanParserTests-context.xml");
		Object step = context.getBean("s1", Step.class);
		assertNotNull(step, "Step not parsed");
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		Object chunkProcessor = ReflectionTestUtils.getField(tasklet, "chunkProcessor");
		assertTrue(chunkProcessor instanceof FaultTolerantChunkProcessor<?, ?>, "Wrong processor type");
	}

	// BATCH-1396
	@Test
	void testSkippableLateBindingAttributes() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/ParentSkippableLateBindingStepFactoryBeanParserTests-context.xml");
		Object step = context.getBean("s1", Step.class);
		assertNotNull(step, "Step not parsed");
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		Object chunkProcessor = ReflectionTestUtils.getField(tasklet, "chunkProcessor");
		assertTrue(chunkProcessor instanceof FaultTolerantChunkProcessor<?, ?>, "Wrong processor type");
	}

}
