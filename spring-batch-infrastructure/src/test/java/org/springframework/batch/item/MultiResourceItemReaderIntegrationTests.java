package org.springframework.batch.item;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.FieldSet;
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Tests for {@link MultiResourceItemReader}.
 */
public class MultiResourceItemReaderIntegrationTests extends TestCase {

	private static final String PATTERN = "resource location pattern";

	private MultiResourceItemReader tested = new MultiResourceItemReader();

	private FlatFileItemReader itemReader = new FlatFileItemReader();

	private ExecutionContext ctx = new ExecutionContext();

	// test input spans several resources
	private Resource r1 = new ByteArrayResource("1\n2\n3\n".getBytes());

	private Resource r2 = new ByteArrayResource("4\n5\n".getBytes());

	private Resource r3 = new ByteArrayResource("".getBytes());

	private Resource r4 = new ByteArrayResource("6\n".getBytes());

	private Resource r5 = new ByteArrayResource("7\n8\n".getBytes());

	/**
	 * Setup the tested reader to read from the test resources.
	 */
	protected void setUp() throws Exception {

		MockControl control = MockControl.createStrictControl(ResourcePatternResolver.class);
		ResourcePatternResolver resolver = (ResourcePatternResolver) control.getMock();
		resolver.getResources(PATTERN);
		control.setReturnValue(new Resource[] { r1, r2, r3, r4, r5 }, 2);
		control.replay();

		itemReader.setFieldSetMapper(new PassThroughFieldSetMapper());

		tested.setResourcePatternResolver(resolver);
		tested.setDelegate(itemReader);
		tested.setResourceLocationPattern(PATTERN);
		tested.afterPropertiesSet();
	}

	/**
	 * Read input from start to end.
	 */
	public void testRead() throws Exception {

		tested.open(ctx);

		assertEquals("1", readItem());
		assertEquals("2", readItem());
		assertEquals("3", readItem());
		assertEquals("4", readItem());
		assertEquals("5", readItem());
		assertEquals("6", readItem());
		assertEquals("7", readItem());
		assertEquals("8", readItem());
		assertEquals(null, readItem());

		tested.close(ctx);
	}

	/**
	 * Read items with a couple of rollbacks, requiring to jump back to items
	 * from previous resources.
	 */
	public void testReset() throws Exception {

		tested.open(ctx);

		assertEquals("1", readItem());

		tested.mark();

		assertEquals("2", readItem());
		assertEquals("3", readItem());

		tested.reset();

		assertEquals("2", readItem());
		assertEquals("3", readItem());
		assertEquals("4", readItem());

		tested.reset();

		assertEquals("2", readItem());
		assertEquals("3", readItem());
		assertEquals("4", readItem());
		assertEquals("5", readItem());

		tested.mark();

		assertEquals("6", readItem());
		assertEquals("7", readItem());

		tested.reset();

		assertEquals("6", readItem());
		assertEquals("7", readItem());

		assertEquals("8", readItem());
		assertEquals(null, readItem());

		tested.close(ctx);
	}

	/**
	 * Restore from saved state.
	 */
	public void testRestart() throws Exception {

		itemReader.setSaveState(true);

		tested.open(ctx);

		assertEquals("1", readItem());
		assertEquals("2", readItem());
		assertEquals("3", readItem());
		assertEquals("4", readItem());

		tested.update(ctx);

		assertEquals("5", readItem());
		assertEquals("6", readItem());

		tested.close(ctx);

		tested.open(ctx);

		assertEquals("5", readItem());
		assertEquals("6", readItem());
		assertEquals("7", readItem());
		assertEquals("8", readItem());
		assertEquals(null, readItem());
	}

	private String readItem() throws Exception {
		Object result = tested.read();
		return result == null ? null : ((FieldSet) result).readString(0);

	}

}
