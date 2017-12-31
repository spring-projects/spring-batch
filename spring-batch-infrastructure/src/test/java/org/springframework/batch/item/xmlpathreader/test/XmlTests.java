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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;
import org.junit.internal.runners.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.xmlpathreader.BasisReader;
import org.springframework.batch.item.xmlpathreader.StaxXmlPathReader;
import org.springframework.batch.item.xmlpathreader.TagReader;
import org.springframework.batch.item.xmlpathreader.attribute.Setter;
import org.springframework.batch.item.xmlpathreader.exceptions.ReaderRuntimeException;
import org.springframework.batch.item.xmlpathreader.path.XmlElementPath;
import org.springframework.batch.item.xmlpathreader.test.entities.Child;
import org.springframework.batch.item.xmlpathreader.test.entities.ChildWithPrefix;
import org.springframework.batch.item.xmlpathreader.test.entities.City;
import org.springframework.batch.item.xmlpathreader.test.entities.DeepChild;
import org.springframework.batch.item.xmlpathreader.test.entities.DeepCity;
import org.springframework.batch.item.xmlpathreader.test.entities.LinkChild;
import org.springframework.batch.item.xmlpathreader.test.entities.TAnnotated;
import org.springframework.batch.item.xmlpathreader.test.entities.TObject;
import org.springframework.batch.item.xmlpathreader.test.entities.TStaticAnnotated;
import org.springframework.batch.item.xmlpathreader.test.entities.TSuper;
import org.springframework.batch.item.xmlpathreader.value.Creator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */

public class XmlTests {
	private static final Logger log = LoggerFactory.getLogger(XmlTests.class);

	private static final String RESOURCES = "src/test/resources/org/springframework/batch/item/xmlpathreader/test/";

	private static final String EXCEPTION_NOT_EXPECTED = "this exception is a error";

	private static final String EXCEPTION_EXPECTED = "Exception expected";

	private static final String KONTOAUSZUG_XML = RESOURCES + "kontoauszug.xml";

	private static final String KONTOAUSZUG_XML2 = RESOURCES + "kontoauszug2.xml";

	private static final String CHILDS_WITH_PREFIX_XML = RESOURCES + "childsWithPrefix.xml";

	private static final String CHILDS_XML = RESOURCES + "childs.xml";

	private static final String WRONG_XML = RESOURCES + "wrongChilds.xml";

	private static final String CHILDSANDCITY_XML = RESOURCES + "childsAndCity.xml";

	private static final String DEEPCHILDS_XML = RESOURCES + "deepChildsAndCity.xml";

	/**
	 * Tests if a correct XML can be loaded with the {@link StaxXmlPathReader}
	 */
	@Test
	public void readerInitialization() {
		StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader();

		staxXmlPathReader.read(KONTOAUSZUG_XML);
		closeTheReader(staxXmlPathReader);
	}

	/**
	 * Test if the {@link TagReader} can analyze a XML file
	 */
	@Test
	public void tagReader() {
		try {
			TagReader reader = new TagReader();

			reader.read(KONTOAUSZUG_XML);
			reader.read();
			reader.source("reader", "Const");
			closeTheReader(reader);
		}
		catch (XMLStreamException e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}

	}

	/**
	 * Tests if the {@link StaxXmlPathReader} can read in the correct object from a file
	 */
	@Test
	public void readAnObjectFromAFile() {
		try {
			StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader();

			staxXmlPathReader.addValue(new XmlElementPath(Const.Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document),
					TObject.class);
			staxXmlPathReader.read(KONTOAUSZUG_XML);
			Object o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof TObject);
			closeTheReader(staxXmlPathReader);
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}

	}

	/**
	 * Tests if the {@link StaxXmlPathReader} can read in the correct object from a {@link Resource}
	 */
	@Test
	public void readAnObjectFromAResource() {
		try {
			StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader();
			Resource kontoauszug = new ClassPathResource(
					"org/springframework/batch/item/xmlpathreader/test/kontoauszug.xml");
			staxXmlPathReader.addValue(new XmlElementPath(Const.Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document),
					TObject.class);
			staxXmlPathReader.read(kontoauszug);
			Object o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof TObject);
			closeTheReader(staxXmlPathReader);
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}

	}

	/**
	 * Tests if the {@link StaxXmlPathReader} can read in the correct object and attribute from a file
	 */
	@Test
	public void readTheName() {
		try {
			StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader();

			staxXmlPathReader.addValue(new XmlElementPath(Const.Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document),
					TObject.class);
			staxXmlPathReader.addAttribute(new XmlElementPath(Const.Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document),
					new XmlElementPath(Const.TxId_Refs_TxDtls_NtryDtls_Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document),
					"Name");

			staxXmlPathReader.read(KONTOAUSZUG_XML);
			Object o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof TObject);
			assertEquals("TID1003", ((TObject) o).getName());
			closeTheReader(staxXmlPathReader);
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}

	}

	/**
	 * Tests if the {@link StaxXmlPathReader} can read in the correct object and attribute from a file
	 */
	@Test
	public void readerWithAttribute() {
		try {
			StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader();

			staxXmlPathReader.addValue(new XmlElementPath(Const.Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document),
					TObject.class);
			staxXmlPathReader.addAttribute(new XmlElementPath(Const.Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document),
					new XmlElementPath(Const.AtCcy_Amt_Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document), "Name");

			staxXmlPathReader.read(KONTOAUSZUG_XML);
			Object o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof TObject);
			assertEquals("EUR", ((TObject) o).getName());
			closeTheReader(staxXmlPathReader);
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}

	}

	/**
	 * Tests if the {@link StaxXmlPathReader} can read in the correct object and attribute of a superclass from a file [
	 * {@link TObject}, {@link TSuper}
	 */
	@Test
	public void readerWithSuperClass() {
		try {
			StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader();

			staxXmlPathReader.addValue(new XmlElementPath(Const.Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document),
					TObject.class);
			staxXmlPathReader.addAttribute(new XmlElementPath(Const.Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document),
					new XmlElementPath(Const.AtCcy_Amt_Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document), "Verwendungszweck");

			staxXmlPathReader.read(KONTOAUSZUG_XML);

			Object o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof TObject);
			assertEquals("EUR", ((TObject) o).getVerwendungszweck());
			closeTheReader(staxXmlPathReader);
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}

	}

	/**
	 * Tests if the {@link StaxXmlPathReader} can read in the correct object and attribute from a file if two attributes
	 * have the same name
	 */
	@Test
	public void readerNoAttributNameConflict() {
		try {
			StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader();

			staxXmlPathReader.addValue(new XmlElementPath(Const.BkToCstmrDbtCdtNtfctn_Document), TObject.class);
			staxXmlPathReader.addAttribute(new XmlElementPath(Const.BkToCstmrDbtCdtNtfctn_Document),
					new XmlElementPath(Const.MsgId_GrpHdr_BkToCstmrDbtCdtNtfctn_Document), "Verwendungszweck");

			staxXmlPathReader.addValue(new XmlElementPath(Const.Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document),
					TObject.class);
			staxXmlPathReader.addAttribute(new XmlElementPath(Const.Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document),
					new XmlElementPath(Const.AtCcy_Amt_Ntry_Ntfctn_BkToCstmrDbtCdtNtfctn_Document), "Verwendungszweck");

			staxXmlPathReader.read(KONTOAUSZUG_XML);
			staxXmlPathReader.read();

			Object ov = staxXmlPathReader.getValueObject(new XmlElementPath(Const.BkToCstmrDbtCdtNtfctn_Document));
			assertNotNull(ov);
			assertTrue(ov instanceof TObject);
			assertEquals("Message-1", ((TObject) ov).getVerwendungszweck());
			closeTheReader(staxXmlPathReader);
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}

	}

	/**
	 * Tests if the {@link StaxXmlPathReader} can read in the correct object and attribute from a annotated class
	 */
	@Test
	public void readAnnotatedClass() {
		try {
			StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader(TAnnotated.class);
			staxXmlPathReader.read(KONTOAUSZUG_XML);
			Object o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof TAnnotated);
			assertEquals("EUR", ((TAnnotated) o).getName());
			closeTheReader(staxXmlPathReader);
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}

	}

	/**
	 * Tests if the {@link StaxXmlPathReader} can read in the correct object and attribute from a annotated class
	 */
	@Test
	public void readAnnotatedClass2() {
		try {
			StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader(TAnnotated.class);
			staxXmlPathReader.read(KONTOAUSZUG_XML2);
			Object o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof TAnnotated);
			assertEquals("Thomas", ((TAnnotated) o).getName());
			closeTheReader(staxXmlPathReader);
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}

	}

	/**
	 * Tests if the {@link StaxXmlPathReader} can read in the correct object and attribute from a static annotated class
	 */
	@Test
	public void readStaticAnnotatedClass() {
		try {
			StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader(TStaticAnnotated.class);
			staxXmlPathReader.read(KONTOAUSZUG_XML);
			Object o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof TStaticAnnotated);
			assertEquals("EUR", ((TStaticAnnotated) o).getName());
			assertEquals("Thomas", ((TStaticAnnotated) o).getVorName());
			closeTheReader(staxXmlPathReader);
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}

	}

	/**
	 * Tests if the {@link StaxXmlPathReader} throws a exception if the xml is wrong
	 */
	@Test
	public void readWrongXml() {
		try {
			StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader(Child.class);
			staxXmlPathReader.read(WRONG_XML);
			Object o = staxXmlPathReader.read();
			while (o != null) {
				o = staxXmlPathReader.read();
			}
			fail(EXCEPTION_EXPECTED);
			closeTheReader(staxXmlPathReader);
		}
		catch (Exception e) {
			log.error(EXCEPTION_EXPECTED, e);
		}
	}

	/**
	 * Tests if the {@link TagReader} throws a exception if the xml is wrong
	 */
	@Test
	public void readWrongXml2() {
		TagReader reader = new TagReader();
		try {
			reader.read(WRONG_XML);
			reader.read();
			fail(EXCEPTION_EXPECTED);
		}
		catch (ReaderRuntimeException e) {
			log.error(EXCEPTION_EXPECTED, e);
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}
	}

	/**
	 * Tests if the {@link StaxXmlPathReader} can read a XML file with namespace prefixes
	 */
	@Test
	public void readChildsWithPrefix() {
		StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader(ChildWithPrefix.class);
		try {
			staxXmlPathReader.read(CHILDS_WITH_PREFIX_XML);
			Object o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof ChildWithPrefix);
			assertEquals("Hans", ((ChildWithPrefix) o).getName());
			o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof ChildWithPrefix);
			assertEquals("Frank", ((ChildWithPrefix) o).getName());
			o = staxXmlPathReader.read();
			assertNull(o);
			closeTheReader(staxXmlPathReader);
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}
	}

	/**
	 * Tests if the {@link StaxXmlPathReader} can correctly read CHILDS_XML with {@link Child}
	 */
	@Test
	public void readChilds() {
		StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader(Child.class);
		staxXmlPathReader.read(CHILDS_XML);
		try {
			Object o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof Child);
			assertEquals("Vera", ((Child) o).getName());
			o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof Child);
			assertEquals("Thomas", ((Child) o).getName());

			o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof Child);
			assertEquals("Hans", ((Child) o).getName());
			o = staxXmlPathReader.read();
			assertNull(o);
			closeTheReader(staxXmlPathReader);
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}
	}

	/**
	 * Tests if the {@link StaxXmlPathReader} can correctly read CHILDS_XML with {@link Creator} and {@link Setter}
	 * initialization of the reader.
	 */
	@Test
	public void readChildsWithCreatorAndSetter() {
		StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader();
		staxXmlPathReader.addValue("child", Child.class, Child::new);
		staxXmlPathReader.addAttribute("child", "name", Child::setName);
		staxXmlPathReader.read(CHILDS_XML);
		try {
			Object o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof Child);
			assertEquals("Vera", ((Child) o).getName());
			o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof Child);
			assertEquals("Thomas", ((Child) o).getName());

			o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof Child);
			assertEquals("Hans", ((Child) o).getName());
			o = staxXmlPathReader.read();
			assertNull(o);
			closeTheReader(staxXmlPathReader);
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}
	}

	/**
	 * Tests if the {@link StaxXmlPathReader} can correctly read CHILDS_XML with {@link LinkChild}
	 */
	@Test
	public void readLinkChilds() {
		try {
			StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader(LinkChild.class);
			staxXmlPathReader.read(CHILDS_XML);
			Object o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof LinkChild);
			assertEquals("Vera", ((LinkChild) o).getName());

			o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof LinkChild);
			assertEquals("Thomas", ((LinkChild) o).getName());

			o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof LinkChild);
			assertEquals("Hans", ((LinkChild) o).getName());
			closeTheReader(staxXmlPathReader);
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}

	}

	/**
	 * Tests if the {@link StaxXmlPathReader} can correctly read CHILDSANDCITY_XML with {@link LinkChild} and
	 * {@link City}
	 */
	@Test
	public void readLinkChildsAndCity() {
		try {
			StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader(LinkChild.class, City.class);
			staxXmlPathReader.read(CHILDSANDCITY_XML);

			Object o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof City);
			assertEquals("A", ((City) o).getName());

			o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof City);
			assertEquals("C", ((City) o).getName());

			o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof City);
			assertEquals("D", ((City) o).getName());

			o = staxXmlPathReader.read();
			assertTrue(o instanceof LinkChild);
			assertEquals("Vera", ((LinkChild) o).getName());
			assertEquals("C", ((LinkChild) o).getCity().getName());
			assertEquals("D", ((LinkChild) o).getSecond().getName());

			o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof LinkChild);
			assertEquals("Thomas", ((LinkChild) o).getName());

			o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof LinkChild);
			assertEquals("Hans", ((LinkChild) o).getName());
			assertEquals("A", ((LinkChild) o).getCity().getName());

			assertEquals("Vera", ((LinkChild) o).getChild().getChild().getName());
			closeTheReader(staxXmlPathReader);
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}

	}

	/**
	 * Tests if the {@link StaxXmlPathReader} can correctly read DEEPCHILDS_XML with {@link DeepChild} and
	 * {@link DeepCity}
	 */
	@Test
	public void readDeepChildsAndCity() {
		try {
			StaxXmlPathReader staxXmlPathReader = new StaxXmlPathReader(DeepChild.class, DeepCity.class);
			staxXmlPathReader.read(DEEPCHILDS_XML);

			Object o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof DeepCity);
			assertEquals("A", ((DeepCity) o).getName());

			o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof DeepCity);
			assertEquals("C", ((DeepCity) o).getName());

			o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof DeepCity);
			assertEquals("D", ((DeepCity) o).getName());

			o = staxXmlPathReader.read();
			assertTrue(o instanceof DeepChild);
			assertEquals("Vera", ((DeepChild) o).getName());
			assertEquals("C", ((DeepChild) o).getCity().getName());
			assertEquals("D", ((DeepChild) o).getSecond().getName());

			o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof DeepChild);
			assertEquals("Thomas", ((DeepChild) o).getName());

			o = staxXmlPathReader.read();
			assertNotNull(o);
			assertTrue(o instanceof DeepChild);
			assertEquals("Hans", ((DeepChild) o).getName());
			assertEquals("A", ((DeepChild) o).getCity().getName());

			assertEquals("Vera", ((DeepChild) o).getChild().getChild().getName());
			closeTheReader(staxXmlPathReader);
		}
		catch (Exception e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}

	}

	private void closeTheReader(BasisReader staxXmlPathReader) {
		try {
			staxXmlPathReader.close();
		}
		catch (XMLStreamException e) {
			log.error(EXCEPTION_NOT_EXPECTED, e);
			fail(EXCEPTION_NOT_EXPECTED);
		}
	}

}
