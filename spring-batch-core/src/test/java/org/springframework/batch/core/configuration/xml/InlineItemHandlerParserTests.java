/*
 * Copyright 2009-2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.adapter.ItemProcessorAdapter;
import org.springframework.batch.item.adapter.ItemReaderAdapter;
import org.springframework.batch.item.adapter.ItemWriterAdapter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Dan Garrette
 * @since 2.1
 */
class InlineItemHandlerParserTests {

	private ConfigurableApplicationContext context;

	@AfterEach
	void close() {
		if (context != null) {
			context.close();
		}
		StepSynchronizationManager.release();
	}

	@Test
	void testInlineHandlers() {
		context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/InlineItemHandlerParserTests-context.xml");
		Object step = context.getBean("inlineHandlers");
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		Object chunkProvider = ReflectionTestUtils.getField(tasklet, "chunkProvider");
		Object reader = ReflectionTestUtils.getField(chunkProvider, "itemReader");
		Object chunkProcessor = ReflectionTestUtils.getField(tasklet, "chunkProcessor");
		Object processor = ReflectionTestUtils.getField(chunkProcessor, "itemProcessor");
		Object writer = ReflectionTestUtils.getField(chunkProcessor, "itemWriter");

		assertTrue(reader instanceof TestReader);
		assertTrue(processor instanceof TestProcessor);
		assertTrue(writer instanceof TestWriter);
	}

	@Test
	void testInlineAdapters() {
		context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/InlineItemHandlerParserTests-context.xml");
		Object step = context.getBean("inlineAdapters");
		Object tasklet = ReflectionTestUtils.getField(step, "tasklet");
		Object chunkProvider = ReflectionTestUtils.getField(tasklet, "chunkProvider");
		Object reader = ReflectionTestUtils.getField(chunkProvider, "itemReader");
		Object chunkProcessor = ReflectionTestUtils.getField(tasklet, "chunkProcessor");
		Object processor = ReflectionTestUtils.getField(chunkProcessor, "itemProcessor");
		Object writer = ReflectionTestUtils.getField(chunkProcessor, "itemWriter");

		assertTrue(reader instanceof ItemReaderAdapter<?>);
		Object readerObject = ReflectionTestUtils.getField(reader, "targetObject");
		assertTrue(readerObject instanceof DummyItemHandlerAdapter);
		Object readerMethod = ReflectionTestUtils.getField(reader, "targetMethod");
		assertEquals("dummyRead", readerMethod);

		assertTrue(processor instanceof ItemProcessorAdapter<?, ?>);
		Object processorObject = ReflectionTestUtils.getField(processor, "targetObject");
		assertTrue(processorObject instanceof DummyItemHandlerAdapter);
		Object processorMethod = ReflectionTestUtils.getField(processor, "targetMethod");
		assertEquals("dummyProcess", processorMethod);

		assertTrue(writer instanceof ItemWriterAdapter<?>);
		Object writerObject = ReflectionTestUtils.getField(writer, "targetObject");
		assertTrue(writerObject instanceof DummyItemHandlerAdapter);
		Object writerMethod = ReflectionTestUtils.getField(writer, "targetMethod");
		assertEquals("dummyWrite", writerMethod);
	}

	@Test
	void testInlineHandlersWithStepScope() {
		context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/InlineItemHandlerWithStepScopeParserTests-context.xml");
		StepSynchronizationManager.register(new StepExecution("step", new JobExecution(123L)));

		@SuppressWarnings({ "rawtypes" })
		Map<String, ItemReader> readers = context.getBeansOfType(ItemReader.class);
		// Should be 2 each (proxy and target) for the two readers in the steps defined
		assertEquals(4, readers.size());
	}

}
