package org.springframework.batch.item.file;

import java.util.Comparator;

import junit.framework.TestCase;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.mapping.FieldSet;
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * Tests for {@link MultiResourceItemReader}.
 */
public class MultiResourceItemReaderIntegrationTests extends TestCase {

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

		itemReader.setFieldSetMapper(new PassThroughFieldSetMapper());

		tested.setDelegate(itemReader);
		tested.setComparator(new Comparator() {
			public int compare(Object o1, Object o2) {
				return 0; // do not change ordering
			}});
		tested.setResources(new Resource[] { r1, r2, r3, r4, r5 });
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
		tested.setSaveState(true);

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

	/**
	 * Resources are ordered according to injected comparator.
	 */
	public void testResourceOrderingWithCustomComparator() {
		
		Resource r1 = new ByteArrayResource("".getBytes(), "b");
		Resource r2 = new ByteArrayResource("".getBytes(), "a");
		Resource r3 = new ByteArrayResource("".getBytes(), "c");
		
		
		Resource[] resources = new Resource[] {r1, r2, r3};
		
		Comparator comp = new Comparator() {

			/**
			 * Reversed ordering by filename.
			 */
			public int compare(Object o1, Object o2) {
				Resource r1 = (Resource) o1;
				Resource r2 = (Resource) o2;
				return -r1.getDescription().compareTo(r2.getDescription());
			}

		};

		tested.setComparator(comp);
		tested.setResources(resources);
		tested.open(ctx);

		assertSame(r3, resources[0]);
		assertSame(r1, resources[1]);
		assertSame(r2, resources[2]);
	}

	private String readItem() throws Exception {
		Object result = tested.read();
		return result == null ? null : ((FieldSet) result).readString(0);

	}

}
