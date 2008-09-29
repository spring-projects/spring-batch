/**
 * 
 */
package org.springframework.batch.sample.domain.trade;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.file.mapping.DefaultFieldSet;
import org.springframework.batch.item.file.mapping.FieldSet;
import org.springframework.batch.item.file.transform.LineTokenizer;

/**
 * @author Lucas Ward
 *
 */
public class CompositeCustomerUpdateLineTokenizerTests {

	StubLineTokenizer customerTokenizer;
	FieldSet customerFieldSet = new DefaultFieldSet(null);
	StubLineTokenizer footerTokenizer;
	FieldSet footerFieldSet = new DefaultFieldSet(null);
	CompositeCustomerUpdateLineTokenizer compositeTokenizer;
	
	@Before
	public void init(){
		customerTokenizer = new StubLineTokenizer(customerFieldSet);
		footerTokenizer = new StubLineTokenizer(footerFieldSet);
		compositeTokenizer = new CompositeCustomerUpdateLineTokenizer();
		compositeTokenizer.setCustomerTokenizer(customerTokenizer);
		compositeTokenizer.setFooterTokenizer(footerTokenizer);
	}
	
	@Test
	public void testFooter(){
		
		String footerLine = "Ffjkdalsfjdaskl;f";
		FieldSet fs = compositeTokenizer.tokenize(footerLine);
		assertEquals(footerFieldSet, fs);
		assertEquals(footerLine, footerTokenizer.getTokenizedLine());
	}
	
	@Test
	public void testCustomerAdd(){
		
		String customerAddLine = "AFDASFDASFDFSA";
		FieldSet fs = compositeTokenizer.tokenize(customerAddLine);
		assertEquals(customerFieldSet, fs);
		assertEquals(customerAddLine, customerTokenizer.getTokenizedLine());
	}
	
	@Test
	public void testCustomerDelete(){
		
		String customerAddLine = "DFDASFDASFDFSA";
		FieldSet fs = compositeTokenizer.tokenize(customerAddLine);
		assertEquals(customerFieldSet, fs);
		assertEquals(customerAddLine, customerTokenizer.getTokenizedLine());
	}
	
	@Test
	public void testCustomerUpdate(){
		
		String customerAddLine = "UFDASFDASFDFSA";
		FieldSet fs = compositeTokenizer.tokenize(customerAddLine);
		assertEquals(customerFieldSet, fs);
		assertEquals(customerAddLine, customerTokenizer.getTokenizedLine());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testInvalidLine(){
		
		String invalidLine = "INVALID";
		compositeTokenizer.tokenize(invalidLine);
	}
	
	
	private static class StubLineTokenizer implements LineTokenizer{

		private final FieldSet fieldSetToReturn;
		private String tokenizedLine;
		
		public StubLineTokenizer(FieldSet fieldSetToReturn) {
			this.fieldSetToReturn = fieldSetToReturn;
		}
		
		public FieldSet tokenize(String line) {
			this.tokenizedLine = line;
			return fieldSetToReturn;
		}
		
		public String getTokenizedLine() {
			return tokenizedLine;
		}
	}
}
