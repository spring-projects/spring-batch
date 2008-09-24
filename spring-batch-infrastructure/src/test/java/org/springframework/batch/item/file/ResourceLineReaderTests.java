package org.springframework.batch.item.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.separator.RecordSeparatorPolicy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;

/**
 * Tests for {@link ResourceLineReader}.
 */
public class ResourceLineReaderTests {

	private ResourceLineReader tested;

	private static final Resource resource = new ByteArrayResource("line1\nline2\nline3\nline4\nline5".getBytes());

	private ExecutionContext executionContext = new ExecutionContext();

	private static ResourceLineReader getItemReader() {
		ResourceLineReader result = new ResourceLineReader();
		result.setResource(resource);
		return result;
	}

	@Before
	public void setUp() throws Exception {
		tested = getItemReader();
		tested.open(executionContext);
	}

	/**
	 * Regular scenario - read the input and eventually return null.
	 */
	@Test
	public void testRead() throws Exception {

		assertEquals("line1", tested.read());
		assertEquals("line2", tested.read());
		assertEquals("line3", tested.read());
		assertEquals("line4", tested.read());
		assertEquals("line5", tested.read());

		assertNull(tested.read());
	}

	/**
	 * No input should be handled gracefully - null is returned on first
	 * read.
	 */
	@Test
	public void testNoInput() throws Exception {
		tested = getItemReader();
		tested.setResource(new DescriptiveResource("doesn't exist") {

			@Override
			public boolean exists() {
				return false;
			}
			
		});
		tested.open(executionContext);
		assertNull(tested.read());
	}

	/**
	 * Restart scenario - read items, update execution context, create new
	 * reader and restore from restart data - the new input source should
	 * continue where the old one finished.
	 */
	@Test
	public void testRestart() throws Exception {

		tested.update(executionContext);

		assertEquals("line1", tested.read());
		assertEquals("line2", tested.read());

		tested.update(executionContext);

		// create new input source
		tested = getItemReader();

		tested.open(executionContext);

		assertEquals("line3", tested.read());
	}

	/**
	 * Restart scenario - read items, rollback to last marked position, update
	 * execution context, create new reader and restore from restart data - the
	 * new input source should continue where the old one finished.
	 */
	@Test
	public void testResetAndRestart() throws Exception {

		tested.update(executionContext);

		assertEquals("line1", tested.read());

		assertEquals("line2", tested.read());

		tested.update(executionContext);

		assertEquals("line3", tested.read());

		// create new input source
		tested = getItemReader();

		tested.open(executionContext);

		assertEquals("line3", tested.read());
	}

	@Test
	public void testReopen() throws Exception {
		tested.update(executionContext);

		assertEquals("line1", tested.read());
		assertEquals("line2", tested.read());

		tested.update(executionContext);

		
		tested.close(executionContext);
		tested.open(executionContext);

		assertEquals("line3", tested.read());
	}
	
	@Test
	public void testRestartWithCustomRecordSeparatorPolicy() throws Exception {
		
		tested.setRecordSeparatorPolicy(new RecordSeparatorPolicy() {
			// 1 record = 2 lines
			boolean pair = true;

			public boolean isEndOfRecord(String line) {
				pair = !pair;
				return pair;
			}

			public String postProcess(String record) {
				return record;
			}

			public String preProcess(String record) {
				return record;
			}
		});

		tested.open(executionContext);

		assertEquals("line1line2", tested.read());
		
		tested.update(executionContext);
		tested.close(executionContext);
		tested.open(executionContext);
		
		assertEquals("line3line4", tested.read());
		
	}
	
}
