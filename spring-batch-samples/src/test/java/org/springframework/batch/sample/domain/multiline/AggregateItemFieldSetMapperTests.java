package org.springframework.batch.sample.domain.multiline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.sample.domain.multiline.AggregateItemFieldSetMapper;

public class AggregateItemFieldSetMapperTests {

	private AggregateItemFieldSetMapper<String> mapper = new AggregateItemFieldSetMapper<String>();

	@Test
	public void testDefaultBeginRecord() throws Exception {
		assertTrue(mapper.mapFieldSet(new DefaultFieldSet(new String[] { "BEGIN" })).isHeader());
		assertFalse(mapper.mapFieldSet(new DefaultFieldSet(new String[] { "BEGIN" })).isFooter());
	}

	@Test
	public void testSetBeginRecord() throws Exception {
		mapper.setBegin("FOO");
		assertTrue(mapper.mapFieldSet(new DefaultFieldSet(new String[] { "FOO" })).isHeader());
	}

	@Test
	public void testDefaultEndRecord() throws Exception {
		assertFalse(mapper.mapFieldSet(new DefaultFieldSet(new String[] { "END" })).isHeader());
		assertTrue(mapper.mapFieldSet(new DefaultFieldSet(new String[] { "END" })).isFooter());
	}

	@Test
	public void testSetEndRecord() throws Exception {
		mapper.setEnd("FOO");
		assertTrue(mapper.mapFieldSet(new DefaultFieldSet(new String[] { "FOO" })).isFooter());
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
			public String mapFieldSet(FieldSet fs) {
				return "foo";
			}
		});
		assertEquals("foo", mapper.mapFieldSet(new DefaultFieldSet(new String[] { "FOO" })).getItem());
	}


}
