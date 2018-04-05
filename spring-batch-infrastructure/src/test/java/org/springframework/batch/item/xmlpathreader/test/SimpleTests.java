/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.batch.item.xmlpathreader.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.xml.namespace.QName;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.xmlpathreader.adapters.AdapterMap;
import org.springframework.batch.item.xmlpathreader.adapters.BooleanAdapter;
import org.springframework.batch.item.xmlpathreader.adapters.DoubleAdapter;
import org.springframework.batch.item.xmlpathreader.adapters.FloatAdapter;
import org.springframework.batch.item.xmlpathreader.adapters.IntegerAdapter;
import org.springframework.batch.item.xmlpathreader.adapters.LongAdapter;
import org.springframework.batch.item.xmlpathreader.attribute.Attribute;
import org.springframework.batch.item.xmlpathreader.attribute.AttributeWithAdapter;
import org.springframework.batch.item.xmlpathreader.attribute.AttributeWithValue;
import org.springframework.batch.item.xmlpathreader.attribute.Setter;
import org.springframework.batch.item.xmlpathreader.attribute.SetterAttribute;
import org.springframework.batch.item.xmlpathreader.core.ValuesAndAttributesContainer;
import org.springframework.batch.item.xmlpathreader.exceptions.ReaderRuntimeException;
import org.springframework.batch.item.xmlpathreader.nls.Messages;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPath;
import org.springframework.batch.item.xmlpathreader.test.entities.TObject;
import org.springframework.batch.item.xmlpathreader.utils.ClassUtils;
import org.springframework.batch.item.xmlpathreader.value.ClassValue;
import org.springframework.batch.item.xmlpathreader.value.CreatorValue;
import org.springframework.batch.item.xmlpathreader.value.CurrentObject;
import org.springframework.batch.item.xmlpathreader.value.SimpleCurrentObject;
import org.springframework.batch.item.xmlpathreader.value.StackCurrentObject;
import org.springframework.batch.item.xmlpathreader.value.Value;
import org.springframework.batch.item.xmlpathreader.value.ValueContainer;

/**
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */

public class SimpleTests {
	private static final String EXCEPTION_NOT_EXPECTED = "this exception is a error";

	private static final String EXCEPTION_EXPECTED = "Exception expected";

	private static final Logger log = LoggerFactory.getLogger(SimpleTests.class);

	/**
	 * Tests the {@link ClassUtils} method
	 */
	@Test
	public void testClassUtils() {
		assertFalse(ClassUtils.isThisClassOrASuperClass(Void.class, Object.class));
		assertFalse(ClassUtils.isThisClassOrASuperClass(Object.class, Void.class));
		assertTrue(ClassUtils.isThisClassOrASuperClass(TObject.class, Object.class));
		assertTrue(ClassUtils.isThisClassOrASuperClass(Double.class, Number.class));
		assertFalse(ClassUtils.isThisClassOrASuperClass(Double.class, TObject.class));
	}

	/**
	 * Tests the NLS Support, throw a {@link ReaderRuntimeException}
	 */
	@Test
	public void testMessagesRuntimeException() {
		try {
			IllegalArgumentException cause = new IllegalArgumentException();
			Messages.throwReaderRuntimeException(cause, "Runtime.FILE_PROCESSING");
			fail(EXCEPTION_EXPECTED);
		}
		catch (ReaderRuntimeException ex) {
			log.error(EXCEPTION_EXPECTED, ex);
		}
		catch (Exception ex) {
			fail(EXCEPTION_NOT_EXPECTED);
			log.error(EXCEPTION_NOT_EXPECTED, ex);
		}
	}

	/**
	 * Tests the NLS Support, throw a {@link IllegalArgumentException}
	 */
	@Test
	public void testMessagesIllegalArgument() {
		try {
			Messages.throwIllegalArgumentException("Runtime.FILE_PROCESSING");
			fail(EXCEPTION_EXPECTED);
		}
		catch (IllegalArgumentException ex) {
			log.error(EXCEPTION_EXPECTED, ex);
		}
		catch (Exception ex) {
			fail(EXCEPTION_NOT_EXPECTED);
			log.error(EXCEPTION_NOT_EXPECTED, ex);
		}
	}

	/**
	 * Tests non existent messages
	 */
	@Test
	public void testNonExistentMessages() {
		try {
			IllegalArgumentException cause = new IllegalArgumentException();
			Messages.throwReaderRuntimeException(cause, "NotExists");
			fail(EXCEPTION_EXPECTED);
		}
		catch (Exception ex) {
			log.error(EXCEPTION_EXPECTED, ex);
		}
	}

	/**
	 * Tests namespace parsing namespace/prefix/localName
	 */
	@Test
	public void testXmlElementPath_NPL() {
		XmlElementPath e = new XmlElementPath("/{namespace}prefix:localName");
		QName[] parts = { new QName("namespace", "localName", "prefix") };
		assertEquals(e, new XmlElementPath(parts));
	}

	/**
	 * Tests namespace parsing prefix/localName
	 */
	@Test
	public void testXmlElementPath_PL() {
		XmlElementPath e = new XmlElementPath("/prefix:localName");
		QName[] parts = { new QName(null, "localName", "prefix") };
		assertEquals(e, new XmlElementPath(parts));
	}

	/**
	 * Tests namespace parsing localName
	 */
	@Test
	public void testXmlElementPath_L() {
		XmlElementPath e = new XmlElementPath("/localName");
		QName[] parts = { new QName("localName") };
		assertEquals(e, new XmlElementPath(parts));
	}

	/**
	 * Tests XmlElementPath.startsWith
	 */
	@Test
	public void testXmlElementPath_StartWithPaths() {
		XmlElementPath a = new XmlElementPath("/aa/bb/cc");
		XmlElementPath b = new XmlElementPath("/a");
		XmlElementPath c = new XmlElementPath("/aa");
		XmlElementPath d = new XmlElementPath("/aa/bb");
		assertTrue(a.startsWith(a));
		assertTrue(a.startsWith(c));
		assertTrue(a.startsWith(d));
		assertFalse(a.startsWith(b));
	}

	/**
	 * Tests XmlElementPath.endsWith
	 */
	@Test
	public void testXmlElementPath_EndWithPaths() {
		XmlElementPath a = new XmlElementPath("/aa/bb/cc");
		XmlElementPath b = new XmlElementPath("/c");
		XmlElementPath c1 = new XmlElementPath("/cc");
		XmlElementPath d1 = new XmlElementPath("/bb/cc");
		XmlElementPath c2 = new XmlElementPath("cc");
		XmlElementPath d2 = new XmlElementPath("bb/cc");
		XmlElementPath d3 = new XmlElementPath("/ee/aa/bb/cc");
		XmlElementPath d4 = new XmlElementPath("/ee/ee/ee");
		assertTrue(a.endsWith(a));
		assertFalse(a.endsWith(b));
		assertFalse(a.endsWith(c1));
		assertFalse(a.endsWith(d1));
		assertTrue(a.endsWith(c2));
		assertTrue(a.endsWith(d2));
		assertFalse(a.endsWith(d3));
		assertFalse(a.endsWith(d4));
	}

	/**
	 * Tests XmlElementPath.compare
	 */
	@Test
	public void testXmlElementPath_Compare() {
		XmlElementPath absolut = new XmlElementPath("/a/b/c");
		XmlElementPath relativ = new XmlElementPath("b/c");

		assertTrue(absolut.isAbsolut());
		assertFalse(relativ.isAbsolut());

		assertTrue(absolut.compare(relativ));
		assertFalse(absolut.compare(new XmlElementPath("a")));
		assertTrue(absolut.compare(absolut));
		assertFalse(absolut.compare(new XmlElementPath("/a")));

	}

	/**
	 * Tests XmlElementPath.parent
	 */

	@Test
	public void testXmlElementPath_parent() {
		XmlElementPath child = new XmlElementPath("/a/b/cc");
		XmlElementPath parent = new XmlElementPath("/a/b");

		assertEquals(parent, child.parent());

	}

	/**
	 * Create a method attribute and set a it to "Test"
	 */
	@Test
	public void methodAttribute() {
		CurrentObject current = new SimpleCurrentObject();
		Value value = new ClassValue(new XmlElementPath("/value"), TObject.class, current, new SimpleCurrentObject());
		value.push();

		ValuesAndAttributesContainer container = new ValuesAndAttributesContainer(current);

		Attribute action = container.createAttribute(value, new XmlElementPath("/value/first"), "Name");
		action.setValue("Test");

		value.pop();
		TObject testObject = (TObject) current.popCurrentObject();

		assertEquals("Test", testObject.getName());

	}

	/**
	 * Create a {@link Setter}, {@link SetterAttribute} and set a it to "Test"
	 */
	@Test
	public void creatorSetter() {
		CurrentObject current = new SimpleCurrentObject();
		Value value = new CreatorValue(new XmlElementPath("/value"), TObject.class, current, new SimpleCurrentObject(),
				TObject::new);
		value.push();

		ValuesAndAttributesContainer container = new ValuesAndAttributesContainer(current);

		Attribute action = container.addAttribute(value, new XmlElementPath("/value/first"), TObject::setName);
		action.setValue("Test");

		value.pop();
		TObject testObject = (TObject) current.popCurrentObject();

		assertEquals("Test", testObject.getName());

	}

	/**
	 * Create a method attribute and set a it to "Test" with is of a float
	 */
	@Test
	public void methodHandleWithWrongValue() {
		CurrentObject current = new SimpleCurrentObject();
		Value value = new ClassValue(new XmlElementPath("/value"), TObject.class, current, new SimpleCurrentObject());
		value.push();

		ValuesAndAttributesContainer container = new ValuesAndAttributesContainer(current);

		Attribute action = container.createAttribute(value, new XmlElementPath("/value/first"), "fValue");

		try {
			action.setValue("Test");
			fail(EXCEPTION_EXPECTED);
		}
		catch (ReaderRuntimeException e) {
			log.error(EXCEPTION_EXPECTED, e);
		}
		value.pop();
	}

	/**
	 * A method attribute, but the {@link Value} has no Object
	 */
	@Test
	public void methodHandleWithEmptyValue() {
		CurrentObject current = new SimpleCurrentObject();
		Value value = new ClassValue(new XmlElementPath("/value"), TObject.class, current, new SimpleCurrentObject());
		// no value.push() create a value for /value

		ValuesAndAttributesContainer container = new ValuesAndAttributesContainer(current);

		Attribute action = container.createAttribute(value, new XmlElementPath("/value/first"), "Name");

		try {
			action.setValue("Test");
			fail(EXCEPTION_EXPECTED);
		}
		catch (ReaderRuntimeException e) {
			log.error(EXCEPTION_EXPECTED, e);
		}
	}

	/**
	 * A method attribute, with a deeper path /value/first/is
	 */
	@Test
	public void methodHandleWithDeeperPath() {
		CurrentObject current = new SimpleCurrentObject();
		Value value = new ClassValue(new XmlElementPath("/value"), TObject.class, current, new StackCurrentObject());
		value.push();

		ValuesAndAttributesContainer container = new ValuesAndAttributesContainer(current);

		Attribute action = container.createAttribute(value, new XmlElementPath("/value/first/is"), "Name");
		action.setValue("Test");

		value.pop();
		TObject testObject = (TObject) current.popCurrentObject();

		assertEquals("Test", testObject.getName());

	}

	/**
	 * Tests the {@link ValuesAndAttributesContainer} methods: push,pop,popCurrentObject
	 */
	@Test
	public void testValuesAndAttributesContainerPushPop() {

		CurrentObject current = new SimpleCurrentObject();
		Value value = new ClassValue(new XmlElementPath("/first/is"), TObject.class, current, new SimpleCurrentObject());
		ValueContainer map = new ValueContainer();

		ValuesAndAttributesContainer container = new ValuesAndAttributesContainer(current, map);
		container.addValue(value);
		container.addAttribute(container.createAttribute(value, new XmlElementPath("name"), "Name"));

		container.push(new QName("first"));
		container.push(new QName("is"));
		container.push(new QName("name"));
		container.setText("Test");
		container.pop();
		container.pop();

		TObject testObject = (TObject) current.popCurrentObject();

		assertEquals("Test", testObject.getName());

	}

	/**
	 * Tests the marshalling of the Adapters in {@link org.springframework.batch.item.xmlpathreader.adapters}
	 */
	@Test
	public void adapterMarshallTests() {
		try {
			DoubleAdapter dAdapter = new DoubleAdapter();
			assertEquals("2.0", dAdapter.marshal(new Double(2.0)));

			FloatAdapter fAdapter = new FloatAdapter();
			assertEquals("2.0", fAdapter.marshal(new Float(2.0)));

			IntegerAdapter iAdapter = new IntegerAdapter();
			assertEquals("2", iAdapter.marshal(new Integer(2)));

			LongAdapter lAdapter = new LongAdapter();
			assertEquals("2", lAdapter.marshal(new Long(2L)));

			BooleanAdapter bAdapter = new BooleanAdapter();
			assertEquals("true", bAdapter.marshal(Boolean.TRUE));

		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}
	}

	/**
	 * Tests the unmarshalling of the Adapters in {@link org.springframework.batch.item.xmlpathreader.adapters}
	 */
	@Test
	public void adapterUnmarshallTests() {
		try {
			DoubleAdapter dAdapter = new DoubleAdapter();
			assertEquals(new Double(2.0), dAdapter.unmarshal("2.0"));

			FloatAdapter fAdapter = new FloatAdapter();
			assertEquals(new Float(2.0), fAdapter.unmarshal("2.0"));

			IntegerAdapter iAdapter = new IntegerAdapter();
			assertEquals(new Integer(2), iAdapter.unmarshal("2"));

			LongAdapter lAdapter = new LongAdapter();
			assertEquals(new Long(2L), lAdapter.unmarshal("2"));

			BooleanAdapter bAdapter = new BooleanAdapter();
			assertEquals(Boolean.TRUE, bAdapter.unmarshal("true"));
			assertEquals(Boolean.FALSE, bAdapter.unmarshal("false"));
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}

	}

	/**
	 * Tests the {@link AttributeWithAdapter}
	 */
	@Test
	public void adapter() {

		CurrentObject current = new SimpleCurrentObject();
		Value value = new ClassValue(new XmlElementPath("/first"), TObject.class, current, new SimpleCurrentObject());

		ValuesAndAttributesContainer container = new ValuesAndAttributesContainer(current);

		Attribute iSetter = container.createAttribute(value, new XmlElementPath("/first/n"), "Nummer");
		Attribute bSetter = container.createAttribute(value, new XmlElementPath("/first/b"), "bValue");
		Attribute fSetter = container.createAttribute(value, new XmlElementPath("/first/f"), "fValue");
		Attribute dSetter = container.createAttribute(value, new XmlElementPath("/first/d"), "dValue");
		Attribute lSetter = container.createAttribute(value, new XmlElementPath("/first/l"), "lValue");

		value.push();
		iSetter.setValue("66");
		lSetter.setValue("888");
		fSetter.setValue("6.6");
		dSetter.setValue("6.6");
		bSetter.setValue("true");

		value.pop();
		TObject testObject = (TObject) current.popCurrentObject();
		assertEquals(66, testObject.getNummer());
		assertEquals(888l, testObject.getlValue());
		assertEquals(6.6d, testObject.getdValue(), 0.01);
		assertEquals(6.6f, testObject.getfValue(), 0.01);
		assertEquals(true, testObject.isbValue());

	}

}
