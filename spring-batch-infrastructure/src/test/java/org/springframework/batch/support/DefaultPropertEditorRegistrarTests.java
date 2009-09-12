/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.support;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Properties;

import org.junit.Test;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.beans.propertyeditors.PropertiesEditor;

public class DefaultPropertEditorRegistrarTests {

	@Test
	public void testIntArray() throws Exception {
		DefaultPropertyEditorRegistrar mapper = new DefaultPropertyEditorRegistrar();
		BeanWithIntArray result = new BeanWithIntArray();
		mapper.setCustomEditors(Collections.singletonMap(int[].class, new IntArrayPropertyEditor()));
		BeanWrapperImpl wrapper = new BeanWrapperImpl(result);
		mapper.registerCustomEditors(wrapper);
		PropertiesEditor editor = new PropertiesEditor();
		editor.setAsText("numbers=1,2,3, 4");
		Properties props = (Properties) editor.getValue();
		wrapper.setPropertyValues(props);
		assertEquals(4, result.numbers[3]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetCustomEditorsWithInvalidTypeName() throws Exception {

		DefaultPropertyEditorRegistrar mapper = new DefaultPropertyEditorRegistrar();
		mapper.setCustomEditors(Collections.singletonMap("FOO", new CustomNumberEditor(Long.class, true)));
	}

	@Test
	public void testSetCustomEditorsWithStringTypeName() throws Exception {

		DefaultPropertyEditorRegistrar mapper = new DefaultPropertyEditorRegistrar();
		mapper.setCustomEditors(Collections.singletonMap("java.lang.Long", new CustomNumberEditor(Long.class, true)));
		BeanWithIntArray result = new BeanWithIntArray();
		BeanWrapperImpl wrapper = new BeanWrapperImpl(result);
		mapper.registerCustomEditors(wrapper);
		wrapper.setPropertyValues(new MutablePropertyValues(Collections.singletonMap("number", "123")));
		assertEquals(123L, result.number);

	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetCustomEditorsWithInvalidType() throws Exception {

		DefaultPropertyEditorRegistrar mapper = new DefaultPropertyEditorRegistrar();
		mapper.setCustomEditors(Collections.singletonMap(new Object(), new CustomNumberEditor(Long.class, true)));
	}

	@SuppressWarnings("unused")
	private static class BeanWithIntArray {
		private int[] numbers;

		private long number;

		
		public void setNumbers(int[] numbers) {
			this.numbers = numbers;
		}

		public void setNumber(long number) {
			this.number = number;
		}
	}

}
