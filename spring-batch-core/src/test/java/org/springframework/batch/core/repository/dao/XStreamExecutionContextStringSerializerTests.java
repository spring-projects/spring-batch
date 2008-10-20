package org.springframework.batch.core.repository.dao;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.Before;

import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.math.BigDecimal;

/**
 * @author trisberg
 */
public class XStreamExecutionContextStringSerializerTests {

	ExecutionContextStringSerializer serializer;

	@Before
	public void onSetUp() throws Exception {
		serializer = new XStreamExecutionContextStringSerializer();
		((XStreamExecutionContextStringSerializer)serializer).afterPropertiesSet();
	}

	@Test
	public void testSerializeAMap() {
		Map<String, Object> m1 = new HashMap<String, Object>();
		m1.put("object1", Long.valueOf(12345L));
		m1.put("object2", "OBJECT TWO");
		m1.put("object3", new Date(1234567L));
		m1.put("object4", new Double(1234567.1234D));

		String s = serializer.serialize(m1);

		Map<String, Object> m2 = serializer.deserialize(s);

		compareContexts(m1, m2);
	}

	@Test
	public void testComplexObject() {
		Map<String, Object> m1 = new HashMap<String, Object>();
		ComplexObject o1 = new ComplexObject();
		o1.setName("Test");
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("object1", Long.valueOf(12345L));
		m.put("object2", "OBJECT TWO");
		o1.setMap(m);
		o1.setNumber(new BigDecimal("12345.67"));
		ComplexObject o2 = new ComplexObject();
		o2.setName("Inner Object");
		o2.setMap(m);
		o2.setNumber(new BigDecimal("98765.43"));
		o1.setObj(o2);
		m1.put("co", o1);

		String s = serializer.serialize(m1);

		Map<String, Object> m2 = serializer.deserialize(s);

		compareContexts(m1, m2);
	}

	private void compareContexts(Map<String, Object> m1, Map<String, Object> m2) {
		for (String key : m1.keySet()) {
			assertEquals("Bad key/value for " + key, m1.get(key), m2.get(key));
		}
	}

}
