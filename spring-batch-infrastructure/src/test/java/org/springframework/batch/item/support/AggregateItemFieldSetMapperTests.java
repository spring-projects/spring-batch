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
		assertTrue(mapper.process(new DefaultFieldSet(new String[] { "BEGIN" })).isHeader());
		assertFalse(mapper.process(new DefaultFieldSet(new String[] { "BEGIN" })).isFooter());
	}

	@Test
	public void testSetBeginRecord() throws Exception {
		mapper.setBegin("FOO");
		assertTrue(mapper.process(new DefaultFieldSet(new String[] { "FOO" })).isHeader());
	}

	@Test
	public void testDefaultEndRecord() throws Exception {
		assertFalse(mapper.process(new DefaultFieldSet(new String[] { "END" })).isHeader());
		assertTrue(mapper.process(new DefaultFieldSet(new String[] { "END" })).isFooter());
	}

	@Test
	public void testSetEndRecord() throws Exception {
		mapper.setEnd("FOO");
		assertTrue(mapper.process(new DefaultFieldSet(new String[] { "FOO" })).isFooter());
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
			public String process(FieldSet fs) {
				return "foo";
			}
		});
		assertEquals("foo", mapper.process(new DefaultFieldSet(new String[] { "FOO" })).getItem());
	}


}
