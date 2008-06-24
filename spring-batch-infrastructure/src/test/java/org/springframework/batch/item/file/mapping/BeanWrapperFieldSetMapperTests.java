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

package org.springframework.batch.item.file.mapping;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.beans.propertyeditors.PropertiesEditor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

public class BeanWrapperFieldSetMapperTests extends TestCase {

	public void testNameAndTypeSpecified() throws Exception {
		BeanWrapperFieldSetMapper mapper = new BeanWrapperFieldSetMapper();
		mapper.setTargetType(TestObject.class);
		mapper.setPrototypeBeanName("foo");
		try {
			mapper.afterPropertiesSet();
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

	public void testNameNorTypeSpecified() throws Exception {
		BeanWrapperFieldSetMapper mapper = new BeanWrapperFieldSetMapper();
		try {
			mapper.afterPropertiesSet();
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

	public void testVanillaBeanCreatedFromType() throws Exception {
		BeanWrapperFieldSetMapper mapper = new BeanWrapperFieldSetMapper();
		mapper.setTargetType(TestObject.class);
		mapper.afterPropertiesSet();

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "This is some dummy string", "true", "C" },
				new String[] { "varString", "varBoolean", "varChar" });
		TestObject result = (TestObject) mapper.mapLine(fieldSet);
		assertEquals("This is some dummy string", result.getVarString());
		assertEquals(true, result.isVarBoolean());
		assertEquals('C', result.getVarChar());
	}

	public void testMapperWithSingleton() throws Exception {
		BeanWrapperFieldSetMapper mapper = new BeanWrapperFieldSetMapper();
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		context.getBeanFactory().registerSingleton("bean", new TestObject());
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "This is some dummy string", "true", "C" },
				new String[] { "varString", "varBoolean", "varChar" });
		TestObject result = (TestObject) mapper.mapLine(fieldSet);
		assertEquals("This is some dummy string", result.getVarString());
		assertEquals(true, result.isVarBoolean());
		assertEquals('C', result.getVarChar());
	}

	public void testPropertyNameMatching() throws Exception {
		BeanWrapperFieldSetMapper mapper = new BeanWrapperFieldSetMapper();
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		context.getBeanFactory().registerSingleton("bean", new TestObject());
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "This is some dummy string", "true", "C" },
				new String[] { "VarString", "VAR_BOOLEAN", "VAR_CHAR" });
		TestObject result = (TestObject) mapper.mapLine(fieldSet);
		assertEquals("This is some dummy string", result.getVarString());
		assertEquals(true, result.isVarBoolean());
		assertEquals('C', result.getVarChar());
	}

	public void testMapperWithPrototype() throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext("bean-wrapper.xml", getClass());

		BeanWrapperFieldSetMapper mapper = (BeanWrapperFieldSetMapper) context.getBean("fieldSetMapper");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "This is some dummy string", "true", "C" },
				new String[] { "varString", "varBoolean", "varChar" });
		TestObject result = (TestObject) mapper.mapLine(fieldSet);
		assertEquals("This is some dummy string", result.getVarString());
		assertEquals(true, result.isVarBoolean());
		assertEquals('C', result.getVarChar());

	}

	public void testMapperWithNestedBeanPaths() throws Exception {
		TestNestedA testNestedA = new TestNestedA();
		TestNestedB testNestedB = new TestNestedB();
		testNestedA.setTestObjectB(testNestedB);
		testNestedB.setTestObjectC(new TestNestedC());

		BeanWrapperFieldSetMapper mapper = new BeanWrapperFieldSetMapper();
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		context.getBeanFactory().registerSingleton("bean", testNestedA);
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(
				new String[] { "This is some dummy string", "1", "Another dummy", "2" }, new String[] { "valueA",
						"valueB", "testObjectB.valueA", "testObjectB.testObjectC.value" });

		TestNestedA result = (TestNestedA) mapper.mapLine(fieldSet);

		assertEquals("This is some dummy string", result.getValueA());
		assertEquals(1, result.getValueB());
		assertEquals("Another dummy", result.getTestObjectB().getValueA());
		assertEquals(2, result.getTestObjectB().getTestObjectC().getValue());
	}

	public void testMapperWithSimilarNamePropertyMatches() throws Exception {
		TestNestedA testNestedA = new TestNestedA();

		BeanWrapperFieldSetMapper mapper = new BeanWrapperFieldSetMapper();
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		context.getBeanFactory().registerSingleton("bean", testNestedA);
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "This is some dummy string", "1" }, new String[] {
				"VALUE_A", "VALUE_B" });

		TestNestedA result = (TestNestedA) mapper.mapLine(fieldSet);

		assertEquals("This is some dummy string", result.getValueA());
		assertEquals(1, result.getValueB());
	}

	public void testMapperWithNotVerySimilarNamePropertyMatches() throws Exception {
		TestNestedC testNestedC = new TestNestedC();

		BeanWrapperFieldSetMapper mapper = new BeanWrapperFieldSetMapper();
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		context.getBeanFactory().registerSingleton("bean", testNestedC);
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "1" }, new String[] { "foo" });

		TestNestedC result = (TestNestedC) mapper.mapLine(fieldSet);

		// "foo" is similar enough to "value" that it matches - but only because
		// nothing else does...
		assertEquals(1, result.getValue());
	}

	public void testMapperWithNestedBeanPathsAndPropertyMatches() throws Exception {
		TestNestedA testNestedA = new TestNestedA();
		TestNestedB testNestedB = new TestNestedB();
		testNestedA.setTestObjectB(testNestedB);
		testNestedB.setTestObjectC(new TestNestedC());

		BeanWrapperFieldSetMapper mapper = new BeanWrapperFieldSetMapper();
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		context.getBeanFactory().registerSingleton("bean", testNestedA);
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "Another dummy", "2" }, new String[] {
				"TestObjectB.ValueA", "TestObjectB.TestObjectC.Value" });

		TestNestedA result = (TestNestedA) mapper.mapLine(fieldSet);

		assertEquals("Another dummy", result.getTestObjectB().getValueA());
		assertEquals(2, result.getTestObjectB().getTestObjectC().getValue());
	}

	public void testMapperWithNestedBeanPathsAndPropertyMisMatches() throws Exception {
		TestNestedA testNestedA = new TestNestedA();
		TestNestedB testNestedB = new TestNestedB();
		testNestedA.setTestObjectB(testNestedB);

		BeanWrapperFieldSetMapper mapper = new BeanWrapperFieldSetMapper();
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		context.getBeanFactory().registerSingleton("bean", testNestedA);
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "Another dummy" }, new String[] { "TestObjectB.foo" });

		try {
			mapper.mapLine(fieldSet);
			fail("Expected NotWritablePropertyException");
		}
		catch (NotWritablePropertyException e) {
			// expected
		}
	}

	public void testMapperWithNestedBeanPathsAndPropertyPrefixMisMatches() throws Exception {
		TestNestedA testNestedA = new TestNestedA();
		TestNestedB testNestedB = new TestNestedB();
		testNestedA.setTestObjectB(testNestedB);

		BeanWrapperFieldSetMapper mapper = new BeanWrapperFieldSetMapper();
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		context.getBeanFactory().registerSingleton("bean", testNestedA);
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "2" }, new String[] { "TestObjectA.garbage" });

		try {
			mapper.mapLine(fieldSet);
			fail("Expected NotWritablePropertyException");
		}
		catch (NotWritablePropertyException e) {
			// expected
		}
	}

	public void testPlainBeanWrapper() throws Exception {
		TestObject result = new TestObject();
		BeanWrapperImpl wrapper = new BeanWrapperImpl(result);
		PropertiesEditor editor = new PropertiesEditor();
		editor.setAsText("varString=This is some dummy string\nvarBoolean=true\nvarChar=C");
		Properties props = (Properties) editor.getValue();
		wrapper.setPropertyValues(props);
		assertEquals("This is some dummy string", result.getVarString());
		assertEquals(true, result.isVarBoolean());
		assertEquals('C', result.getVarChar());
	}

	// BeanWrapperFieldSetMapper doesn't currently support nesting with
	// collections.
	public void testNestedList() {

		TestNestedList nestedList = new TestNestedList();
		List nestedC = new ArrayList();
		nestedC.add(new TestNestedC());
		nestedC.add(new TestNestedC());
		nestedC.add(new TestNestedC());
		nestedList.setNestedC(nestedC);

		BeanWrapperFieldSetMapper mapper = new BeanWrapperFieldSetMapper();
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		context.getBeanFactory().registerSingleton("bean", nestedList);
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "1", "2", "3" }, new String[] { "NestedC[0].Value",
				"NestedC[1].Value", "NestedC[2].Value" });

		mapper.mapLine(fieldSet);

		assertEquals(1, ((TestNestedC) nestedList.getNestedC().get(0)).getValue());
		assertEquals(2, ((TestNestedC) nestedList.getNestedC().get(1)).getValue());
		assertEquals(3, ((TestNestedC) nestedList.getNestedC().get(2)).getValue());

	}

	public void testPaddedLongWithNoEditor() throws Exception {

		BeanWrapperFieldSetMapper mapper = new BeanWrapperFieldSetMapper();
		mapper.setTargetType(TestObject.class);

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "00009" }, new String[] { "varLong" });
		TestObject bean = (TestObject) mapper.mapLine(fieldSet);
		// since Spring 2.5.5 this is OK (before that BATCH-261)
		assertEquals(9, bean.getVarLong());
	}

	public void testPaddedLongWithEditor() throws Exception {

		BeanWrapperFieldSetMapper mapper = new BeanWrapperFieldSetMapper();
		mapper.setTargetType(TestObject.class);

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "00009" }, new String[] { "varLong" });

		mapper.setCustomEditors(Collections.singletonMap(Long.TYPE, new CustomNumberEditor(Long.class, NumberFormat
				.getNumberInstance(), true)));
		TestObject bean = (TestObject) mapper.mapLine(fieldSet);

		assertEquals(9, bean.getVarLong());
	}

	public void testPaddedLongWithDefaultAndCustomEditor() throws Exception {

		BeanWrapperFieldSetMapper mapper = new BeanWrapperFieldSetMapper();
		mapper.setTargetType(TestObject.class);

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "00009", "78" }, new String[] { "varLong", "varInt" });

		mapper.setCustomEditors(Collections.singletonMap(Long.TYPE, new CustomNumberEditor(Long.class, NumberFormat
				.getNumberInstance(), true)));
		TestObject bean = (TestObject) mapper.mapLine(fieldSet);

		assertEquals(9, bean.getVarLong());
		assertEquals(78, bean.getVarInt());
	}

	private static class TestNestedList {

		List nestedC;

		public List getNestedC() {
			return nestedC;
		}

		public void setNestedC(List nestedC) {
			this.nestedC = nestedC;
		}

	}

	private static class TestNestedA {
		private String valueA;

		private int valueB;

		TestNestedB testObjectB;

		public TestNestedB getTestObjectB() {
			return testObjectB;
		}

		public void setTestObjectB(TestNestedB testObjectB) {
			this.testObjectB = testObjectB;
		}

		public String getValueA() {
			return valueA;
		}

		public void setValueA(String valueA) {
			this.valueA = valueA;
		}

		public int getValueB() {
			return valueB;
		}

		public void setValueB(int valueB) {
			this.valueB = valueB;
		}

	}

	private static class TestNestedB {
		private String valueA;

		private TestNestedC testObjectC;

		public TestNestedC getTestObjectC() {
			return testObjectC;
		}

		public void setTestObjectC(TestNestedC testObjectC) {
			this.testObjectC = testObjectC;
		}

		public String getValueA() {
			return valueA;
		}

		public void setValueA(String valueA) {
			this.valueA = valueA;
		}

	}

	private static class TestNestedC {
		private int value;

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}
	}

}
