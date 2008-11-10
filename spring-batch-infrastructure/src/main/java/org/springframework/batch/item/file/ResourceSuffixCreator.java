package org.springframework.batch.item.file;

/**
 * Strategy interface for translating resource index into unique filename
 * suffix.
 *
 * @see MultiResourceItemWriter
 * @see SimpleResourceSuffixCreator 
 * 
 * @author Robert Kasanicky
 */
public interface ResourceSuffixCreator {

	String getSuffix(int index);
}
