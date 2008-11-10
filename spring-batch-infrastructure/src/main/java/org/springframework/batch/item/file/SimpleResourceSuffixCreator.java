package org.springframework.batch.item.file;

/**
 * Trivial implementation of {@link ResourceSuffixCreator} that uses the index
 * itself as suffix, separated by dot.
 * 
 * @author Robert Kasanicky
 */
public class SimpleResourceSuffixCreator implements ResourceSuffixCreator {

	public String getSuffix(int index) {
		return "." + index;
	}

}
