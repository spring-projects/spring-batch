/**
 * 
 */
package org.springframework.batch.item.file.transform;

/**
 * Factory interface for creating {@link FieldSet} instances.
 * 
 * @author Dave Syer
 *
 */
public interface FieldSetFactory {
	
	FieldSet create(String[] names, String[] values);

	FieldSet create(String[] values);

}
