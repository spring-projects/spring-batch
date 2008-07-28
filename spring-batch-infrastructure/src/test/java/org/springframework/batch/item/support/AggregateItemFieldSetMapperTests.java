package org.springframework.batch.item.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.batch.item.file.mapping.DefaultFieldSet;
import org.springframework.batch.item.file.mapping.FieldSet;
import org.springframework.batch.item.file.mapping.FieldSetMapper;

public class AggregateItemFieldSetMapperTests {

	private AggregateItemFieldSetMapper<String> mapper = new AggregateItemFieldSetMapper<String>();

	@Test
	public void testDefaultBeginRecord() throws Exception {
		assertTrue(mapper.mapLine(new DefaultFieldSet(new String[] { "BEGIN" }), -1).isHeader());
		assertFalse(mapper.mapLine(new DefaultFieldSet(new String[] { "BEGIN" }), -1).isFooter());
	}

	@Test
	public void testSetBeginRecord() throws Exception {
		mapper.setBegin("FOO");
		assertTrue(mapper.mapLine(new DefaultFieldSet(new String[] { "FOO" }), -1).isHeader());
	}

	@Test
	public void testDefaultEndRecord() throws Exception {
		assertFalse(mapper.mapLine(new DefaultFieldSet(new String[] { "END" }), -1).isHeader());
		assertTrue(mapper.mapLine(new DefaultFieldSet(new String[] { "END" }), -1).isFooter());
	}

	@Test
	public void testSetEndRecord() throws Exception {
		mapper.setEnd("FOO");
		assertTrue(mapper.mapLine(new DefaultFieldSet(new String[] { "FOO" }), -1).isFooter());
	}

	@Test
	public void testMandatoryProperties() throws Exception {
		try {
			mapper.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testDelegate() throws Exception {
		mapper.setDelegate(new FieldSetMapper<String>() {
			public String mapLine(FieldSet fs, int lineNum) {
				return "foo";
			}
		});
		assertEquals("foo", mapper.mapLine(new DefaultFieldSet(new String[] { "FOO" }), -1).getItem());
	}


}
