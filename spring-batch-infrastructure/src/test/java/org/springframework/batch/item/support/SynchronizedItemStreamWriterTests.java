package org.springframework.batch.item.support;

import org.junit.Test;

/**
 *
 * @author Dimitrios Liapis
 *
 */
public class SynchronizedItemStreamWriterTests extends AbstractSynchronizedItemStreamWriterTests {

	@Test(expected = AssertionError.class)
	public void givenMultipleThreads_whenAllCallItemStreamWriter_thenNotThreadSafe() throws Exception {
		TestItemWriter testItemWriter = new TestItemWriter();
		multiThreadedInvocation(testItemWriter);
	}

	@Test
	public void givenMultipleThreads_whenAllCallSynchronizedItemStreamWriter_thenThreadSafe() throws Exception {
		TestItemWriter testItemWriter = new TestItemWriter();
		SynchronizedItemStreamWriter<Integer> synchronizedItemStreamWriter = new SynchronizedItemStreamWriter<>();
		synchronizedItemStreamWriter.setDelegate(testItemWriter);
		multiThreadedInvocation(synchronizedItemStreamWriter);
	}

}
