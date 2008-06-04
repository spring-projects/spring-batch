package org.springframework.batch.item;

import java.util.Arrays;
import java.util.Comparator;

import org.springframework.core.io.Resource;

/**
 * {@link MultiResourceItemReader} which orders the injected resources using
 * {@link #setComparator(Comparator)} to avoid potential problems caused by
 * resource re-ordering on restart.
 * 
 * @see MultiResourceItemReader
 * 
 * @author Robert Kasanicky
 */
public class SortedMultiResourceItemReader extends MultiResourceItemReader {

	private Comparator comparator = new Comparator() {

		/**
		 * Compares resource filenames.
		 */
		public int compare(Object o1, Object o2) {
			Resource r1 = (Resource) o1;
			Resource r2 = (Resource) o2;
			return r1.getFilename().compareTo(r2.getFilename());
		}

	};
	
	public SortedMultiResourceItemReader() {
		setName(SortedMultiResourceItemReader.class.getSimpleName());
	}

	/**
	 * @param comparator used to order the injected resources, by default
	 * compares {@link Resource#getFilename()} values.
	 */
	public void setComparator(Comparator comparator) {
		this.comparator = comparator;
	}

	/**
	 * Orders the resources using {@link #setComparator(Comparator)} before
	 * setting them.
	 */
	public void setResources(Resource[] resources) {
		Arrays.sort(resources, comparator);
		super.setResources(resources);
	}

}
