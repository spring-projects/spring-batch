package org.springframework.batch.item.support.builder;

import org.junit.Test;

import org.springframework.batch.item.support.AbstractSynchronizedItemStreamWriterTests;
import org.springframework.batch.item.support.SynchronizedItemStreamWriter;

/**
 *
 * @author Dimitrios Liapis
 *
 */
public class SynchronizedItemStreamWriterBuilderTests extends AbstractSynchronizedItemStreamWriterTests {

	@Test
	public void givenMultipleThreads_whenAllCallSynchronizedItemStreamWriter_thenThreadSafe() throws Exception {
		TestItemWriter testItemWriter = new TestItemWriter();
		SynchronizedItemStreamWriter<Integer> synchronizedItemStreamWriter =
				new SynchronizedItemStreamWriterBuilder<Integer>()
						.delegate(testItemWriter)
						.build();
		multiThreadedInvocation(synchronizedItemStreamWriter);
	}

}
