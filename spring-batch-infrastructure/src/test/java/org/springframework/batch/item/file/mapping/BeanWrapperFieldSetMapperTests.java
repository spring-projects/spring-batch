/*
 * Copyright 2006-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.file.mapping;

import java.beans.PropertyEditor;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.beans.propertyeditors.PropertiesEditor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.DataBinder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanWrapperFieldSetMapperTests {

	private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

	private final TimeZone defaultTimeZone = TimeZone.getDefault();

	@BeforeEach
	void setUp() {
		TimeZone.setDefault(UTC_TIME_ZONE);
	}

	@AfterEach
	void tearDown() {
		TimeZone.setDefault(defaultTimeZone);
	}

	@Test
	void testNameAndTypeSpecified() {
		BeanWrapperFieldSetMapper<TestObject> mapper = new BeanWrapperFieldSetMapper<>();
		mapper.setTargetType(TestObject.class);
		mapper.setPrototypeBeanName("foo");
		Exception exception = assertThrows(IllegalStateException.class, mapper::afterPropertiesSet);
		assertEquals("Both name and type cannot be specified together.", exception.getMessage());
	}

	@Test
	void testNameNorTypeSpecified() {
		BeanWrapperFieldSetMapper<TestObject> mapper = new BeanWrapperFieldSetMapper<>();
		Exception exception = assertThrows(IllegalStateException.class, mapper::afterPropertiesSet);
		assertEquals("Either name or type must be provided.", exception.getMessage());
	}

	@Test
	void testVanillaBeanCreatedFromType() throws Exception {
		BeanWrapperFieldSetMapper<TestObject> mapper = new BeanWrapperFieldSetMapper<>();
		mapper.setTargetType(TestObject.class);
		mapper.afterPropertiesSet();

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "This is some dummy string", "true", "C" },
				new String[] { "varString", "varBoolean", "varChar" });
		TestObject result = mapper.mapFieldSet(fieldSet);
		assertEquals("This is some dummy string", result.getVarString());
		assertTrue(result.isVarBoolean());
		assertEquals('C', result.getVarChar());
	}

	@Test
	void testNullPropertyAutoCreated() throws Exception {
		BeanWrapperFieldSetMapper<TestNestedA> mapper = new BeanWrapperFieldSetMapper<>();
		mapper.setTargetType(TestNestedA.class);
		mapper.afterPropertiesSet();

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "Foo", "Bar" },
				new String[] { "valueA", "testObjectB.valueA" });
		TestNestedA result = mapper.mapFieldSet(fieldSet);
		assertEquals("Bar", result.getTestObjectB().getValueA());
	}

	@Test
	void testMapperWithSingleton() throws Exception {
		BeanWrapperFieldSetMapper<TestObject> mapper = new BeanWrapperFieldSetMapper<>();
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		context.getBeanFactory().registerSingleton("bean", new TestObject());
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "This is some dummy string", "true", "C" },
				new String[] { "varString", "varBoolean", "varChar" });
		TestObject result = mapper.mapFieldSet(fieldSet);
		assertEquals("This is some dummy string", result.getVarString());
		assertTrue(result.isVarBoolean());
		assertEquals('C', result.getVarChar());
	}

	@Test
	void testPropertyNameMatching() throws Exception {
		BeanWrapperFieldSetMapper<TestObject> mapper = new BeanWrapperFieldSetMapper<>();
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		mapper.setDistanceLimit(2);
		context.getBeanFactory().registerSingleton("bean", new TestObject());
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "This is some dummy string", "true", "C" },
				new String[] { "VarString", "VAR_BOOLEAN", "VAR_CHAR" });
		TestObject result = mapper.mapFieldSet(fieldSet);
		assertEquals("This is some dummy string", result.getVarString());
		assertTrue(result.isVarBoolean());
		assertEquals('C', result.getVarChar());
	}

	@Test
	@SuppressWarnings("unchecked")
	void testMapperWithPrototype() throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext("bean-wrapper.xml", getClass());

		BeanWrapperFieldSetMapper<TestObject> mapper = (BeanWrapperFieldSetMapper<TestObject>) context
			.getBean("fieldSetMapper");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "This is some dummy string", "true", "C" },
				new String[] { "varString", "varBoolean", "varChar" });
		TestObject result = mapper.mapFieldSet(fieldSet);
		assertEquals("This is some dummy string", result.getVarString());
		assertTrue(result.isVarBoolean());
		assertEquals('C', result.getVarChar());

	}

	@Test
	void testMapperWithNestedBeanPaths() throws Exception {
		TestNestedA testNestedA = new TestNestedA();
		TestNestedB testNestedB = new TestNestedB();
		testNestedA.setTestObjectB(testNestedB);
		testNestedB.setTestObjectC(new TestNestedC());

		BeanWrapperFieldSetMapper<TestNestedA> mapper = new BeanWrapperFieldSetMapper<>();
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		context.getBeanFactory().registerSingleton("bean", testNestedA);
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "This is some dummy string", "1", "Another dummy", "2" },
				new String[] { "valueA", "valueB", "testObjectB.valueA", "testObjectB.testObjectC.value" });

		TestNestedA result = mapper.mapFieldSet(fieldSet);

		assertEquals("This is some dummy string", result.getValueA());
		assertEquals(1, result.getValueB());
		assertEquals("Another dummy", result.getTestObjectB().getValueA());
		assertEquals(2, result.getTestObjectB().getTestObjectC().getValue());
	}

	@Test
	void testMapperWithSimilarNamePropertyMatches() throws Exception {
		TestNestedA testNestedA = new TestNestedA();

		BeanWrapperFieldSetMapper<TestNestedA> mapper = new BeanWrapperFieldSetMapper<>();
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		mapper.setDistanceLimit(2);
		context.getBeanFactory().registerSingleton("bean", testNestedA);
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "This is some dummy string", "1" },
				new String[] { "VALUE_A", "VALUE_B" });

		TestNestedA result = mapper.mapFieldSet(fieldSet);

		assertEquals("This is some dummy string", result.getValueA());
		assertEquals(1, result.getValueB());
	}

	@Test
	void testMapperWithNotVerySimilarNamePropertyMatches() throws Exception {
		TestNestedC testNestedC = new TestNestedC();

		BeanWrapperFieldSetMapper<TestNestedC> mapper = new BeanWrapperFieldSetMapper<>();
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		context.getBeanFactory().registerSingleton("bean", testNestedC);
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "1" }, new String[] { "foo" });

		TestNestedC result = mapper.mapFieldSet(fieldSet);

		// "foo" is similar enough to "value" that it matches - but only because
		// nothing else does...
		assertEquals(1, result.getValue());
	}

	@Test
	void testMapperWithNestedBeanPathsAndPropertyMatches() throws Exception {
		TestNestedA testNestedA = new TestNestedA();
		TestNestedB testNestedB = new TestNestedB();
		testNestedA.setTestObjectB(testNestedB);
		testNestedB.setTestObjectC(new TestNestedC());

		BeanWrapperFieldSetMapper<TestNestedA> mapper = new BeanWrapperFieldSetMapper<>();
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		context.getBeanFactory().registerSingleton("bean", testNestedA);
		mapper.setDistanceLimit(2);
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "Another dummy", "2" },
				new String[] { "TestObjectB.ValueA", "TestObjectB.TestObjectC.Value" });

		TestNestedA result = mapper.mapFieldSet(fieldSet);

		assertEquals("Another dummy", result.getTestObjectB().getValueA());
		assertEquals(2, result.getTestObjectB().getTestObjectC().getValue());
	}

	@Test
	void testMapperWithNestedBeanPathsAndPropertyMisMatches() {
		TestNestedA testNestedA = new TestNestedA();
		TestNestedB testNestedB = new TestNestedB();
		testNestedA.setTestObjectB(testNestedB);

		BeanWrapperFieldSetMapper<?> mapper = new BeanWrapperFieldSetMapper<>();
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		context.getBeanFactory().registerSingleton("bean", testNestedA);
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "Another dummy" }, new String[] { "TestObjectB.foo" });

		assertThrows(NotWritablePropertyException.class, () -> mapper.mapFieldSet(fieldSet));
	}

	@Test
	void testMapperWithNestedBeanPathsAndPropertyPrefixMisMatches() {
		TestNestedA testNestedA = new TestNestedA();
		TestNestedB testNestedB = new TestNestedB();
		testNestedA.setTestObjectB(testNestedB);

		BeanWrapperFieldSetMapper<?> mapper = new BeanWrapperFieldSetMapper<>();
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		context.getBeanFactory().registerSingleton("bean", testNestedA);
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "2" }, new String[] { "TestObjectA.garbage" });

		assertThrows(NotWritablePropertyException.class, () -> mapper.mapFieldSet(fieldSet));
	}

	@Test
	void testPlainBeanWrapper() {
		TestObject result = new TestObject();
		BeanWrapperImpl wrapper = new BeanWrapperImpl(result);
		PropertiesEditor editor = new PropertiesEditor();
		editor.setAsText("varString=This is some dummy string\nvarBoolean=true\nvarChar=C");
		Properties props = (Properties) editor.getValue();
		wrapper.setPropertyValues(props);
		assertEquals("This is some dummy string", result.getVarString());
		assertTrue(result.isVarBoolean());
		assertEquals('C', result.getVarChar());
	}

	@Test
	void testNestedList() throws Exception {

		TestNestedList nestedList = new TestNestedList();
		List<TestNestedC> nestedC = new ArrayList<>();
		nestedC.add(new TestNestedC());
		nestedC.add(new TestNestedC());
		nestedC.add(new TestNestedC());
		nestedList.setNestedC(nestedC);

		BeanWrapperFieldSetMapper<?> mapper = new BeanWrapperFieldSetMapper<>();
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		context.getBeanFactory().registerSingleton("bean", nestedList);
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "1", "2", "3" },
				new String[] { "NestedC[0].Value", "NestedC[1].Value", "NestedC[2].Value" });

		mapper.mapFieldSet(fieldSet);

		assertEquals(1, nestedList.getNestedC().get(0).getValue());
		assertEquals(2, nestedList.getNestedC().get(1).getValue());
		assertEquals(3, nestedList.getNestedC().get(2).getValue());

	}

	@Test
	void testAutoPopulateNestedList() throws Exception {
		TestNestedList nestedList = new TestNestedList();

		BeanWrapperFieldSetMapper<?> mapper = new BeanWrapperFieldSetMapper<>() {
			@Override
			protected void initBinder(DataBinder binder) {
				// Use reflection so it compiles (and fails) with Spring 2.5
				ReflectionTestUtils.setField(binder, "autoGrowNestedPaths", true);
			}
		};
		StaticApplicationContext context = new StaticApplicationContext();
		mapper.setBeanFactory(context);
		context.getBeanFactory().registerSingleton("bean", nestedList);
		mapper.setPrototypeBeanName("bean");

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "1", "2", "3" },
				new String[] { "NestedC[0].Value", "NestedC[1].Value", "NestedC[2].Value" });

		mapper.mapFieldSet(fieldSet);

		assertEquals(1, nestedList.getNestedC().get(0).getValue());
		assertEquals(2, nestedList.getNestedC().get(1).getValue());
		assertEquals(3, nestedList.getNestedC().get(2).getValue());

	}

	@Test
	void testPaddedLongWithNoEditor() throws Exception {

		BeanWrapperFieldSetMapper<TestObject> mapper = new BeanWrapperFieldSetMapper<>();
		mapper.setTargetType(TestObject.class);

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "00009" }, new String[] { "varLong" });
		TestObject bean = mapper.mapFieldSet(fieldSet);
		// since Spring 2.5.5 this is OK (before that BATCH-261)
		assertEquals(9, bean.getVarLong());
	}

	@Test
	void testPaddedLongWithEditor() throws Exception {

		BeanWrapperFieldSetMapper<TestObject> mapper = new BeanWrapperFieldSetMapper<>();
		mapper.setTargetType(TestObject.class);

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "00009" }, new String[] { "varLong" });

		mapper.setCustomEditors(Collections.singletonMap(Long.TYPE,
				new CustomNumberEditor(Long.class, NumberFormat.getNumberInstance(), true)));
		TestObject bean = mapper.mapFieldSet(fieldSet);

		assertEquals(9, bean.getVarLong());
	}

	@Test
	void testPaddedLongWithDefaultAndCustomEditor() throws Exception {

		BeanWrapperFieldSetMapper<TestObject> mapper = new BeanWrapperFieldSetMapper<>();
		mapper.setTargetType(TestObject.class);

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "00009", "78" }, new String[] { "varLong", "varInt" });

		mapper.setCustomEditors(Collections.singletonMap(Long.TYPE,
				new CustomNumberEditor(Long.class, NumberFormat.getNumberInstance(), true)));
		TestObject bean = mapper.mapFieldSet(fieldSet);

		assertEquals(9, bean.getVarLong());
		assertEquals(78, bean.getVarInt());
	}

	@Test
	void testNumberFormatWithDefaultAndCustomEditor() throws Exception {

		BeanWrapperFieldSetMapper<TestObject> mapper = new BeanWrapperFieldSetMapper<>();
		mapper.setTargetType(TestObject.class);

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "9.876,1", "7,890.1" },
				new String[] { "varDouble", "varFloat" });

		Map<Class<?>, PropertyEditor> editors = new HashMap<>();
		editors.put(Double.TYPE, new CustomNumberEditor(Double.class, NumberFormat.getInstance(Locale.GERMAN), true));
		editors.put(Float.TYPE, new CustomNumberEditor(Float.class, NumberFormat.getInstance(Locale.UK), true));
		mapper.setCustomEditors(editors);

		TestObject bean = mapper.mapFieldSet(fieldSet);

		assertEquals(9876.1, bean.getVarDouble(), 0.01);
		assertEquals(7890.1, bean.getVarFloat(), 0.01);
	}

	@Test
	void testConversionWithTestConverter() throws Exception {

		BeanWrapperFieldSetMapper<TestObject> mapper = new BeanWrapperFieldSetMapper<>();
		mapper.setTargetType(TestObject.class);

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "SHOULD BE CONVERTED" }, new String[] { "varString" });

		mapper.setConversionService(new TestConversion());
		mapper.afterPropertiesSet();
		TestObject bean = mapper.mapFieldSet(fieldSet);

		assertEquals(bean.getVarString(), "CONVERTED", "Expecting the conversion to have returned \"CONVERTED\"");
	}

	@Test
	void testDefaultConversion() throws Exception {

		BeanWrapperFieldSetMapper<TestObject> mapper = new BeanWrapperFieldSetMapper<>();
		mapper.setTargetType(TestObject.class);

		final String sampleString = "myString";
		Date date = new Date();
		BigDecimal bigDecimal = new BigDecimal(12345L);
		String dateString = date.toString();

		FieldSet fieldSet = new DefaultFieldSet(
				new String[] { "12", "12345", "true", "Z", "123", "12345", "12345", "12", dateString, "12345",
						sampleString },
				new String[] { "varInt", "varLong", "varBoolean", "varChar", "varByte", "varFloat", "varDouble",
						"varShort", "varDate", "varBigDecimal", "varString" });

		mapper.setConversionService(new DefaultConversionService());
		mapper.afterPropertiesSet();

		TestObject bean = mapper.mapFieldSet(fieldSet);

		assertEquals(bean.getVarInt(), 12, "Expected 12 for varInt");
		assertEquals(bean.getVarLong(), 12345L, "Expected 12345 for varLong");
		assertTrue(bean.isVarBoolean(), "Expected true for varBoolean");
		assertEquals(bean.getVarChar(), 'Z', "Expected Z for varChar");
		assertEquals(bean.getVarByte(), 123, "Expected A for varByte");
		assertEquals(bean.getVarFloat(), 12345F, 1F, "Expected 12345 for varFloat");
		assertEquals(bean.getVarDouble(), 12345D, 1D, "Expected 12345 for varDouble");
		assertEquals(bean.getVarShort(), 12, "Expected 12 for varShort");
		assertEquals(bean.getVarDate().toString(), dateString, "Expected currentDate for varDate");
		assertEquals(bean.getVarBigDecimal(), bigDecimal, "Expected 12345 for varBigDecimal");
		assertEquals(bean.getVarString(), sampleString, "Expected " + sampleString + " for varString");

	}

	@Test
	void testConversionAndCustomEditor() {
		BeanWrapperFieldSetMapper<TestObject> mapper = new BeanWrapperFieldSetMapper<>();
		mapper.setTargetType(TestObject.class);

		mapper.setConversionService(new TestConversion());
		mapper.setCustomEditors(Collections.singletonMap(Long.TYPE,
				new CustomNumberEditor(Long.class, NumberFormat.getNumberInstance(), true)));
		Exception exception = assertThrows(IllegalStateException.class, mapper::afterPropertiesSet);
		assertEquals("Both customEditor and conversionService cannot be specified together.", exception.getMessage());
	}

	@Test
	void testBinderWithErrors() {

		BeanWrapperFieldSetMapper<TestObject> mapper = new BeanWrapperFieldSetMapper<>();
		mapper.setTargetType(TestObject.class);

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "foo", "7890.1" },
				new String[] { "varDouble", "varFloat" });
		BindException exception = assertThrows(BindException.class, () -> mapper.mapFieldSet(fieldSet));
		assertEquals(1, exception.getErrorCount());
		assertEquals("typeMismatch", exception.getFieldError("varDouble").getCode());
	}

	@Test
	void testFieldSpecificCustomEditor() throws Exception {

		BeanWrapperFieldSetMapper<TestTwoDoubles> mapper = new BeanWrapperFieldSetMapper<>() {
			@Override
			protected void initBinder(DataBinder binder) {
				binder.registerCustomEditor(Double.TYPE, "value",
						new CustomNumberEditor(Double.class, NumberFormat.getNumberInstance(Locale.GERMAN), true));
			}
		};
		mapper.setTargetType(TestTwoDoubles.class);

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "9.876,1", "7890.1" },
				new String[] { "value", "other" });
		TestTwoDoubles bean = mapper.mapFieldSet(fieldSet);

		assertEquals(9876.1, bean.getValue(), 0.01);
		assertEquals(7890.1, bean.getOther(), 0.01);
	}

	@Test
	void testFieldSpecificCustomEditorWithRegistry() throws Exception {

		BeanWrapperFieldSetMapper<TestTwoDoubles> mapper = new BeanWrapperFieldSetMapper<>() {
			@Override
			public void registerCustomEditors(PropertyEditorRegistry registry) {
				super.registerCustomEditors(registry);
				registry.registerCustomEditor(Double.TYPE, "value",
						new CustomNumberEditor(Double.class, NumberFormat.getNumberInstance(Locale.GERMAN), true));
			}
		};
		mapper.setTargetType(TestTwoDoubles.class);

		FieldSet fieldSet = new DefaultFieldSet(new String[] { "9.876,1", "7890.1" },
				new String[] { "value", "other" });
		TestTwoDoubles bean = mapper.mapFieldSet(fieldSet);

		assertEquals(9876.1, bean.getValue(), 0.01);
		assertEquals(7890.1, bean.getOther(), 0.01);
	}

	@Test
	void testStrict() throws Exception {
		BeanWrapperFieldSetMapper<TestObject> mapper = new BeanWrapperFieldSetMapper<>();
		mapper.setStrict(true);
		mapper.setTargetType(TestObject.class);
		mapper.afterPropertiesSet();

		FieldSet fieldSet = new DefaultFieldSet(
				new String[] { "This is some dummy string", "This won't be mapped", "true", "C" },
				new String[] { "varString", "illegalPropertyName", "varBoolean", "varChar" });
		Exception exception = assertThrows(NotWritablePropertyException.class, () -> mapper.mapFieldSet(fieldSet));
		assertTrue(exception.getMessage().contains("'illegalPropertyName'"));
	}

	@Test
	void testNotStrict() throws Exception {
		BeanWrapperFieldSetMapper<TestObject> mapper = new BeanWrapperFieldSetMapper<>();
		mapper.setStrict(false);
		mapper.setTargetType(TestObject.class);
		mapper.afterPropertiesSet();

		FieldSet fieldSet = new DefaultFieldSet(
				new String[] { "This is some dummy string", "This won't be mapped", "true", "C" },
				new String[] { "varString", "illegalPropertyName", "varBoolean", "varChar" });
		TestObject result = mapper.mapFieldSet(fieldSet);
		assertEquals("This is some dummy string", result.getVarString());
		assertTrue(result.isVarBoolean());
		assertEquals('C', result.getVarChar());
	}

	private static class TestNestedList {

		List<TestNestedC> nestedC = new ArrayList<>();

		public List<TestNestedC> getNestedC() {
			return nestedC;
		}

		public void setNestedC(List<TestNestedC> nestedC) {
			this.nestedC = nestedC;
		}

	}

	public static class TestNestedA {

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

	public static class TestNestedB {

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

	public static class TestNestedC {

		private int value;

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}

	}

	public static class TestTwoDoubles {

		private double value;

		private double other;

		public double getValue() {
			return value;
		}

		public void setValue(double value) {
			this.value = value;
		}

		public double getOther() {
			return other;
		}

		public void setOther(double other) {
			this.other = other;
		}

	}

	public static class TestObject {

		String varString;

		boolean varBoolean;

		char varChar;

		byte varByte;

		short varShort;

		int varInt;

		long varLong;

		float varFloat;

		double varDouble;

		BigDecimal varBigDecimal;

		Date varDate;

		public Date getVarDate() {
			return (Date) varDate.clone();
		}

		public void setVarDate(Date varDate) {
			this.varDate = varDate == null ? null : (Date) varDate.clone();
		}

		public TestObject() {
		}

		public BigDecimal getVarBigDecimal() {
			return varBigDecimal;
		}

		public void setVarBigDecimal(BigDecimal varBigDecimal) {
			this.varBigDecimal = varBigDecimal;
		}

		public boolean isVarBoolean() {
			return varBoolean;
		}

		public void setVarBoolean(boolean varBoolean) {
			this.varBoolean = varBoolean;
		}

		public byte getVarByte() {
			return varByte;
		}

		public void setVarByte(byte varByte) {
			this.varByte = varByte;
		}

		public char getVarChar() {
			return varChar;
		}

		public void setVarChar(char varChar) {
			this.varChar = varChar;
		}

		public double getVarDouble() {
			return varDouble;
		}

		public void setVarDouble(double varDouble) {
			this.varDouble = varDouble;
		}

		public float getVarFloat() {
			return varFloat;
		}

		public void setVarFloat(float varFloat) {
			this.varFloat = varFloat;
		}

		public long getVarLong() {
			return varLong;
		}

		public void setVarLong(long varLong) {
			this.varLong = varLong;
		}

		public short getVarShort() {
			return varShort;
		}

		public void setVarShort(short varShort) {
			this.varShort = varShort;
		}

		public String getVarString() {
			return varString;
		}

		public void setVarString(String varString) {
			this.varString = varString;
		}

		public int getVarInt() {
			return varInt;
		}

		public void setVarInt(int varInt) {
			this.varInt = varInt;
		}

	}

	public static class TestConversion implements ConversionService {

		@Override
		public boolean canConvert(@Nullable Class<?> sourceType, Class<?> targetType) {
			return true;
		}

		@Override
		public boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
			return true;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> @Nullable T convert(@Nullable Object source, Class<T> targetType) {
			return (T) "CONVERTED";
		}

		@Override
		public @Nullable Object convert(@Nullable Object source, @Nullable TypeDescriptor sourceType,
				TypeDescriptor targetType) {
			return "CONVERTED";
		}

	}

}
