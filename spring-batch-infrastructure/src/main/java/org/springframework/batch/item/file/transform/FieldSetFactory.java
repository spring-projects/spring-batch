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
	
	/**
	 * Create a FieldSet with named tokens. The token values can then be
	 * retrieved either by name or by column number.
	 * @param values the token values
	 * @param names the names of the tokens
	 * @see DefaultFieldSet#readString(String)
	 */
	FieldSet create(String[] values, String[] names);

	/**
	 * Create a FieldSet with anonymous tokens. They can only be retrieved by
	 * column number.
	 * @param values the token values
	 * @see FieldSet#readString(int)
	 */
	FieldSet create(String[] values);

}
