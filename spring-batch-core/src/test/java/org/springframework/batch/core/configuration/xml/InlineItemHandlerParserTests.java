package org.springframework.batch.core.configuration.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.After;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
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
public class InlineItemHandlerParserTests {

	private ConfigurableApplicationContext context;

	@After
	public void close() {
		if (context != null) {
			context.close();
		}
		StepSynchronizationManager.release();
	}

	@Test
	public void testInlineHandlers() throws Exception {
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
	public void testInlineAdapters() throws Exception {
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
	public void testInlineHandlersWithStepScope() throws Exception {
		context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/InlineItemHandlerWithStepScopeParserTests-context.xml");
		StepSynchronizationManager.register(new StepExecution("step", new JobExecution(123L)));

		@SuppressWarnings({ "unchecked", "rawtypes" })
		Map<String,ItemReader> readers = context.getBeansOfType(ItemReader.class);
		// Should be 2 each (proxy and target) for the two readers in the steps defined
		assertEquals(4, readers.size());
		// System.err.println(readers);
	}

}
