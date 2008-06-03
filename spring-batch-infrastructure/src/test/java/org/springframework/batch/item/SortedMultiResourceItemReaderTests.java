package org.springframework.batch.item;

import java.util.Comparator;

import junit.framework.TestCase;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Tests for {@link SortedMultiResourceItemReader}.
 */
public class SortedMultiResourceItemReaderTests extends TestCase {

	private SortedMultiResourceItemReader tested = new SortedMultiResourceItemReader();

	private Resource r1 = new FileSystemResource("b");

	private Resource r2 = new FileSystemResource("a");

	private Resource r3 = new FileSystemResource("c");

	private Resource[] resources = { r1, r2, r3 };

	/**
	 * Resources are ordered according to filename by default.
	 */
	public void testResourceOrdering() {

		tested.setResources(resources);

		assertSame(r2, resources[0]);
		assertSame(r1, resources[1]);
		assertSame(r3, resources[2]);

	}

	/**
	 * Resources are ordered according to injected comparator.
	 */
	public void testResourceOrderingWithCustomComparator() {
		Comparator comp = new Comparator() {

			/**
			 * Reversed ordering by filename.
			 */
			public int compare(Object o1, Object o2) {
				Resource r1 = (Resource) o1;
				Resource r2 = (Resource) o2;
				return -r1.getFilename().compareTo(r2.getFilename());
			}

		};

		tested.setComparator(comp);
		tested.setResources(resources);

		assertSame(r3, resources[0]);
		assertSame(r1, resources[1]);
		assertSame(r2, resources[2]);
	}
}
