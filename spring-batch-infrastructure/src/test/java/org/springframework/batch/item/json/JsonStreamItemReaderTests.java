/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.item.json;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.ojm.jackson.JacksonUnmarshaller;

public class JsonStreamItemReaderTests {

	public static class TestObject {
		private Integer id;
		private String string;
		private Double doubleValue;
		private Boolean booleanValue;
		private List<TestObject> nestedTestObjectList;

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return this.id;
		}

		public void getString(String string) {
			this.string = string;
		}

		public String getString() {
			return this.string;
		}

		public void setDoubleValue(Double doubleValue) {
			this.doubleValue = doubleValue;
		}

		public Double getDoubleValue() {
			return this.doubleValue;
		}

		public void setBooleanValue(Boolean booleanValue) {
			this.booleanValue = booleanValue;
		}

		public Boolean getBooleanValue() {
			return this.booleanValue;
		}

		public void setNestedTestObjectList(List<TestObject> nestedTestObjectList) {
			this.nestedTestObjectList = nestedTestObjectList;
		}

		public List<TestObject> getNestedTestObjectList() {
			return this.nestedTestObjectList;
		}
	}

	private void testTemplate(String resourceString, String keyName) throws Exception {
		JacksonUnmarshaller unmarshaller = new JacksonUnmarshaller();
		unmarshaller.setObjectMapper(new ObjectMapper());
		JsonStreamItemReader<JsonStreamItemReaderTests.TestObject> itemReader = new JsonStreamItemReader<JsonStreamItemReaderTests.TestObject>();
		itemReader.setResource(new InputStreamResource(
			ClassLoader.class.getResourceAsStream(resourceString)
		));
		itemReader.setTargetClass(JsonStreamItemReaderTests.TestObject.class);
		itemReader.setUnmarshaller(unmarshaller);
		itemReader.setKeyName(keyName);
		itemReader.afterPropertiesSet();
		itemReader.doOpen();

		TestObject testObject = itemReader.read();
		assertEquals(new Integer(1), testObject.getId());
		assertEquals("a", testObject.getString());
		assertEquals(new Double(0.012), testObject.getDoubleValue());
		assertEquals(true, testObject.getBooleanValue());
		List<TestObject> nestedTestObjects = testObject.getNestedTestObjectList();
		assertEquals(1, nestedTestObjects.size());
		TestObject nestedTestObject0 = nestedTestObjects.get(0);
		assertEquals(null, nestedTestObject0.getId());
		assertEquals("nested-a", nestedTestObject0.getString());
		assertEquals(null, nestedTestObject0.getDoubleValue());
		assertEquals(null, nestedTestObject0.getBooleanValue());

		testObject = itemReader.read();
		assertEquals("\"b", testObject.getString());
		assertEquals(false, testObject.getBooleanValue());
		assertEquals(null, testObject.getNestedTestObjectList());

		testObject = itemReader.read();
		assertEquals(null, testObject.getBooleanValue());
		assertEquals(null, testObject.getNestedTestObjectList());
	}

	@Test
	public void keyName() throws Exception {
		testTemplate("/org/springframework/batch/item/json/keyName.json", "arrayOfObjects");
	}

	@Test
	public void noKeyName() throws Exception {
		testTemplate("/org/springframework/batch/item/json/noKeyName.json", null);		
	}

	@Test
	public void emptyKeyName() throws Exception {
		testTemplate("/org/springframework/batch/item/json/noKeyName.json", "");		
	}
}
