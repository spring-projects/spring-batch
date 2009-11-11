package org.springframework.batch.item.file.separator;

import static org.junit.Assert.*;

import org.junit.Test;

public class JsonRecordSeparatorPolicyTests {
	
	private JsonRecordSeparatorPolicy policy = new JsonRecordSeparatorPolicy();

	@Test
	public void testIsEndOfRecord() {
		assertFalse(policy.isEndOfRecord("{\"a\":\"b\""));
		assertTrue(policy.isEndOfRecord("{\"a\":\"b\"} "));
	}

	@Test
	public void testNestedObject() {
		assertFalse(policy.isEndOfRecord("{\"a\": {\"b\": 2}"));
		assertTrue(policy.isEndOfRecord("{\"a\": {\"b\": 2}} "));
	}

}
