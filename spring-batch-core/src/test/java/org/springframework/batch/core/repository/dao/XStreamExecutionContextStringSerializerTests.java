package org.springframework.batch.core.repository.dao;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Thomas Risberg
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
		// Use a date after 1971 (otherwise daylight saving screws up)...
		m1.put("object3", new Date(123456790123L));
		m1.put("object4", new Double(1234567.1234D));

		String s = serializer.serialize(m1);

		Map<String, Object> m2 = serializer.deserialize(s);

		compareContexts(m1, m2);
	}

	@Test
	public void testComplexObject() {
		Map<String, Object> m1 = new HashMap<String, Object>();
		ComplexObject o1 = new ComplexObject();
		o1.setName("02345");
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

	@SuppressWarnings("unused")
	private static class ComplexObject {
		private String name;
		private BigDecimal number;
		private ComplexObject obj;
		private Map<String,Object> map;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public BigDecimal getNumber() {
			return number;
		}

		public void setNumber(BigDecimal number) {
			this.number = number;
		}

		public ComplexObject getObj() {
			return obj;
		}

		public void setObj(ComplexObject obj) {
			this.obj = obj;
		}

		public Map<String,Object> getMap() {
			return map;
		}

		public void setMap(Map<String,Object> map) {
			this.map = map;
		}


		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			ComplexObject that = (ComplexObject) o;

			if (map != null ? !map.equals(that.map) : that.map != null) return false;
			if (name != null ? !name.equals(that.name) : that.name != null) return false;
			if (number != null ? !number.equals(that.number) : that.number != null) return false;
			if (obj != null ? !obj.equals(that.obj) : that.obj != null) return false;

			return true;
		}

		public int hashCode() {
			int result;
			result = (name != null ? name.hashCode() : 0);
			result = 31 * result + (number != null ? number.hashCode() : 0);
			result = 31 * result + (obj != null ? obj.hashCode() : 0);
			result = 31 * result + (map != null ? map.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "ComplexObject [name=" + name + ", number=" + number + "]";
		}
		
	}
}
