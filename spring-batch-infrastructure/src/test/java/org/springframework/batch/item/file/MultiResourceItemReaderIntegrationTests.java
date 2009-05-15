package org.springframework.batch.item.file;

import java.util.Comparator;

import junit.framework.TestCase;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * Tests for {@link MultiResourceItemReader}.
 */
public class MultiResourceItemReaderIntegrationTests extends TestCase {

	private MultiResourceItemReader<String> tested = new MultiResourceItemReader<String>();

	private FlatFileItemReader<String> itemReader = new FlatFileItemReader<String>();

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

		itemReader.setLineMapper(new PassThroughLineMapper());

		tested.setDelegate(itemReader);
		tested.setComparator(new Comparator<Resource>() {
			public int compare(Resource o1, Resource o2) {
				return 0; // do not change ordering
			}
		});
		tested.setResources(new Resource[] { r1, r2, r3, r4, r5 });
	}

	/**
	 * Read input from start to end.
	 */
	public void testRead() throws Exception {

		tested.open(ctx);

		assertEquals("1", tested.read());
		assertEquals("2", tested.read());
		assertEquals("3", tested.read());
		assertEquals("4", tested.read());
		assertEquals("5", tested.read());
		assertEquals("6", tested.read());
		assertEquals("7", tested.read());
		assertEquals("8", tested.read());
		assertEquals(null, tested.read());

		tested.close();
	}

	public void testGetCurrentResource() throws Exception {

		tested.open(ctx);

		assertSame(r1, tested.getCurrentResource());
		assertEquals("1", tested.read());
		assertSame(r1, tested.getCurrentResource());
		assertEquals("2", tested.read());
		assertSame(r1, tested.getCurrentResource());
		assertEquals("3", tested.read());
		assertSame(r1, tested.getCurrentResource());
		assertEquals("4", tested.read());
		assertSame(r2, tested.getCurrentResource());
		assertEquals("5", tested.read());
		assertSame(r2, tested.getCurrentResource());
		assertEquals("6", tested.read());
		assertSame(r4, tested.getCurrentResource());
		assertEquals("7", tested.read());
		assertSame(r5, tested.getCurrentResource());
		assertEquals("8", tested.read());
		assertSame(r5, tested.getCurrentResource());
		assertEquals(null, tested.read());
		assertSame(null, tested.getCurrentResource());

		tested.close();
	}

	public void testRestartWhenStateNotSaved() throws Exception {

		tested.setSaveState(false);

		tested.open(ctx);

		assertEquals("1", tested.read());

		tested.update(ctx);

		assertEquals("2", tested.read());
		assertEquals("3", tested.read());

		tested.close();

		tested.open(ctx);

		assertEquals("1", tested.read());
	}

	/**
	 * 
	 * Read items with a couple of rollbacks, requiring to jump back to items
	 * from previous resources.
	 */
	public void testRestartAcrossResourceBoundary() throws Exception {

		tested.open(ctx);

		assertEquals("1", tested.read());

		tested.update(ctx);

		assertEquals("2", tested.read());
		assertEquals("3", tested.read());

		tested.close();

		tested.open(ctx);

		assertEquals("2", tested.read());
		assertEquals("3", tested.read());
		assertEquals("4", tested.read());

		tested.close();

		tested.open(ctx);

		assertEquals("2", tested.read());
		assertEquals("3", tested.read());
		assertEquals("4", tested.read());
		assertEquals("5", tested.read());

		tested.update(ctx);

		assertEquals("6", tested.read());
		assertEquals("7", tested.read());

		tested.close();

		tested.open(ctx);

		assertEquals("6", tested.read());
		assertEquals("7", tested.read());

		assertEquals("8", tested.read());
		assertEquals(null, tested.read());

		tested.close();
	}

	/**
	 * Restore from saved state.
	 */
	public void testRestart() throws Exception {

		tested.open(ctx);

		assertEquals("1", tested.read());
		assertEquals("2", tested.read());
		assertEquals("3", tested.read());
		assertEquals("4", tested.read());

		tested.update(ctx);

		assertEquals("5", tested.read());
		assertEquals("6", tested.read());

		tested.close();

		tested.open(ctx);

		assertEquals("5", tested.read());
		assertEquals("6", tested.read());
		assertEquals("7", tested.read());
		assertEquals("8", tested.read());
		assertEquals(null, tested.read());
	}

	/**
	 * Resources are ordered according to injected comparator.
	 */
	public void testResourceOrderingWithCustomComparator() {

		Resource r1 = new ByteArrayResource("".getBytes(), "b");
		Resource r2 = new ByteArrayResource("".getBytes(), "a");
		Resource r3 = new ByteArrayResource("".getBytes(), "c");

		Resource[] resources = new Resource[] { r1, r2, r3 };

		Comparator<Resource> comp = new Comparator<Resource>() {

			/**
			 * Reversed ordering by filename.
			 */
			public int compare(Resource o1, Resource o2) {
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

	/**
	 * Empty resource list is OK.
	 */
	public void testNoResourcesFound() throws Exception {
		tested.setResources(new Resource[] {});
		tested.open(ctx);

		assertNull(tested.read());
	}

}
