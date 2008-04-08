package org.springframework.batch.item;

import org.springframework.batch.item.sample.Foo;

/**
 * Common tests for readers implementing both {@link ItemReader} and
 * {@link ItemStream}. Expected input is five {@link Foo} objects with values 1
 * to 5.
 */
public abstract class CommonItemStreamItemReaderTests extends CommonItemReaderTests {

	protected ExecutionContext executionContext = new ExecutionContext();

	/**
	 * Cast the reader to ItemStream.
	 */
	protected ItemStream testedAsStream() {
		return (ItemStream) tested;
	}

	protected void setUp() throws Exception {
		super.setUp();
		testedAsStream().open(executionContext);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		testedAsStream().close(executionContext);
	}



	/**
	 * Restart scenario - read items, update execution context, create new
	 * reader and restore from restart data - the new input source should
	 * continue where the old one finished.
	 */
	public void testRestart() throws Exception {

		Foo foo1 = (Foo) tested.read();
		assertEquals(1, foo1.getValue());

		Foo foo2 = (Foo) tested.read();
		assertEquals(2, foo2.getValue());

		testedAsStream().update(executionContext);

		// create new input source
		tested = getItemReader();

		testedAsStream().open(executionContext);

		Foo fooAfterRestart = (Foo) tested.read();
		assertEquals(3, fooAfterRestart.getValue());
	}

}
