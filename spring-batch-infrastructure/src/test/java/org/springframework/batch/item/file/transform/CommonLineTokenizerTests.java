package org.springframework.batch.item.file.transform;

import java.util.List;

import junit.framework.TestCase;

/**
 * Tests for {@link AbstractLineTokenizer}.
 * 
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public class CommonLineTokenizerTests extends TestCase {
	
	/**
	 * Columns names are considered to be specified if they are not <code>null</code> or empty.
	 */
	public void testHasNames() {
		AbstractLineTokenizer tokenizer = new AbstractLineTokenizer() {
			protected List<String> doTokenize(String line) {
				return null;
			}
		};
		
		assertFalse(tokenizer.hasNames());
		
		tokenizer.setNames(null);
		assertFalse(tokenizer.hasNames());
		
		tokenizer.setNames(new String[0]);
		assertFalse(tokenizer.hasNames());
		
		tokenizer.setNames(new String[]{"name1", "name2"});
		assertTrue(tokenizer.hasNames());
	}
	
}
