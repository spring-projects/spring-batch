package org.springframework.batch.item.file.transform;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;
import org.springframework.batch.item.file.FlatFileParseException;

public class JsonLineMapperTests {
	
	private JsonLineMapper mapper = new JsonLineMapper();

	@Test
	public void testMapLine() throws Exception {
		Map<String, Object> map = mapper.mapLine("{\"foo\": 1}", 1);
		assertEquals(1, map.get("foo"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testMapNested() throws Exception {
		Map<String, Object> map = mapper.mapLine("{\"foo\": 1, \"bar\" : {\"foo\": 2}}", 1);
		assertEquals(1, map.get("foo"));
		assertEquals(2, ((Map<String, Object>) map.get("bar")).get("foo"));
	}

	@Test(expected=FlatFileParseException.class)
	public void testMappingError() throws Exception {
		Map<String, Object> map = mapper.mapLine("{\"foo\": 1", 1);
		assertEquals(1, map.get("foo"));
	}

}
