/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.ojm.jackson;

import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.Test;

/**
 * Suppressed "unused" warnings on setter methods of test objects.  They are not used 
 * locally but they are used by Jackson through reflection.
 * 
 * @author Matthew Ouyang
 * @since 3.1
 */
public class JacksonUnmarshallerTest {

	private static class SimpleObject {

		private String stringValue;

		public String getStringValue() {
			return stringValue;
		}

		@SuppressWarnings("unused")
		public void setStringValue(String stringValue) {
			this.stringValue = stringValue;
		}
	}

	private static class CompositeObject {

		private String stringValue;
		private List<Integer> arrayOfIntegers;
		private SimpleObject simpleObject;

		public String getStringValue() {
			return stringValue;
		}

		@SuppressWarnings("unused")
		public void setStringValue(String stringValue) {
			this.stringValue = stringValue;
		}

		public List<Integer> getArrayOfIntegers() {
			return arrayOfIntegers;
		}

		@SuppressWarnings("unused")
		public void setArrayOfIntegers(List<Integer> arrayOfIntegers) {
			this.arrayOfIntegers = arrayOfIntegers;
		}

		public SimpleObject getSimpleObject() {
			return simpleObject;
		}

		@SuppressWarnings("unused")
		public void setSimpleObject(SimpleObject simpleObject) {
			this.simpleObject = simpleObject;
		}
	}

	@Test
	public void simpleObject() throws Exception {
		String json = "{'stringValue' : '123'}";
		JacksonUnmarshaller unmarshaller = new JacksonUnmarshaller();
		SimpleObject simpleObject = unmarshaller.unmarshal(new ByteArrayInputStream(json.getBytes()), SimpleObject.class);
		assertThat(simpleObject.getStringValue(), equals("123"));
	}

	@Test
	public void compositeObject() throws Exception {
		String json = "'stringValue' : '321', 'arrayOfIntegers' : [1, 2], 'simpleObject' : {'stringValue' : '123'}";
		JacksonUnmarshaller unmarshaller = new JacksonUnmarshaller();
		CompositeObject simpleObject = unmarshaller.unmarshal(new ByteArrayInputStream(json.getBytes()), CompositeObject.class);
		assertThat(simpleObject.getStringValue(), equals("321"));
		assertThat(simpleObject.getArrayOfIntegers(), hasItems(1, 2));
		assertThat(simpleObject.getSimpleObject().getStringValue(), equals("123"));
	}
}
