package org.springframework.batch.item.file.transform;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class DelimitedLineAggregatorTests {

	private DelimitedLineAggregator<String> aggregator;

	@Before
	public void setup() {
		aggregator = new DelimitedLineAggregator<>();
	}

	@Test
	public void testEmpty() {
		assertEquals("", aggregator.doAggregate(new String[] {}));
	}

	
	@Test
	public void testSingle() {
		assertEquals("aaa", aggregator.doAggregate(new String[] {"aaa"}));
	}
	
	@Test
	public void testTriple() {
		assertEquals("aaa,bbb,ccc", aggregator.doAggregate(new String[] {"aaa", "bbb", "ccc"}));
	}
	
	@Test
	public void testWithQoute() {
		assertEquals("\"aaa\"\"bbb\"", aggregator.doAggregate(new String[] {"aaa\"bbb"}));
	}
	
	@Test
	public void testSingleWithDelimiter() {
		assertEquals("\"aaa,bbb\"", aggregator.doAggregate(new String[] {"aaa,bbb"}));
	}
	
	@Test
	public void testSingleWithNewLine() {
		assertEquals("\"aa\naa\"", aggregator.doAggregate(new String[] {"aa\naa"}));
	}
	
	@Test
	public void testTripleWithNewLine() {
		assertEquals("aaa,\"bb\nbb\",ccc", aggregator.doAggregate(new String[] {"aaa", "bb\nbb", "ccc"}));
	}
	
}
