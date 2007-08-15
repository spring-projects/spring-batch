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

package org.springframework.batch.io.xml.xstream;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.io.exception.BatchEnvironmentException;
import org.springframework.batch.io.xml.ObjectInput;
import org.springframework.batch.io.xml.ObjectOutput;
import org.springframework.batch.io.xml.xstream.AttributeAlias;
import org.springframework.batch.io.xml.xstream.AttributeProperties;
import org.springframework.batch.io.xml.xstream.ClassAlias;
import org.springframework.batch.io.xml.xstream.DefaultImplementation;
import org.springframework.batch.io.xml.xstream.FieldAlias;
import org.springframework.batch.io.xml.xstream.ImplicitCollection;
import org.springframework.batch.io.xml.xstream.OmmitedField;
import org.springframework.batch.io.xml.xstream.TypeAlias;
import org.springframework.batch.io.xml.xstream.XStreamConfiguration;
import org.springframework.batch.io.xml.xstream.XStreamFactory;
import org.springframework.core.io.FileSystemResource;

import com.thoughtworks.xstream.XStream;

/**
 * Integretion test for XStreamFactory.
 * 
 * @author peter.zozom
 */
public class XStreamFactoryIntegrationTests extends TestCase {

	private XStream stream;

	private XStreamConfiguration config;

	private XStreamFactory factory;

	public void testAddDefaultImplementations() throws ClassNotFoundException {

		// override tested methods
		class XStreamExt extends XStream {

			private String diName;

			private String otName;

			private boolean test = false;

			public void init(String diName, String otName) {
				test = true;
				this.diName = diName;
				this.otName = otName;
			}

			public void addDefaultImplementation(Class defaultImplementation, Class ofType) {
				if (test) {
					assertEquals(diName, defaultImplementation.getName());
					assertEquals(otName, ofType.getName());
				}
			}
		}

		// TEST1: test adding default implementation

		// create new XStream
		stream = new XStreamExt();
		// set expected values
		((XStreamExt) stream).init("java.util.ArrayList", "java.util.List");

		// create DefaultImplemetation object
		DefaultImplementation di = new DefaultImplementation();
		di.setDefaultImpl("java.util.ArrayList");
		di.setType("java.util.List");

		// add it to list of defaultImplementations
		List defaultImplementations = new ArrayList();
		defaultImplementations.add(di);

		// create configuration object
		config = new XStreamConfiguration();
		// set list of defaultImplementations
		config.setDefaultImplementations(defaultImplementations);

		// create factory
		factory = new XStreamFactory();
		// set config object
		factory.setConfig(config);
		// call set-up method for XStream
		factory.setUpXStream(stream);

		// TEST2: ClassNotFoundException for 'defaultImplementation' parameter

		// set defaultImplementation class name to some non-existing class name
		di.setDefaultImpl("test.some.nonexisting.ClassName");
		// call set-up method for XStream - BatchEnvironmentException is
		// expected
		try {
			factory.setUpXStream(stream);
			fail("BatchEnvironmentException was expected");
		}
		catch (BatchEnvironmentException bee) {
			assertTrue(bee.getCause() instanceof ClassNotFoundException);
		}

		// TEST3: ClassNotFoundException for 'ofType' parameter

		// set ofType class name to some non-existing class name
		di.setType("test.some.nonexisting.ClassName");
		di.setDefaultImpl("java.util.List");

		// call set-up method for XStream - BatchEnvironmentException is
		// expected
		try {
			factory.setUpXStream(stream);
			fail("BatchEnvironmentException was expected");
		}
		catch (BatchEnvironmentException bee) {
			assertTrue(bee.getCause() instanceof ClassNotFoundException);
		}
	}

	public void testSetClassAliases() {

		// override tested methods
		class XStreamExt extends XStream {

			private String shortName;

			private String typeName;

			private String diName;

			private boolean test;

			public void init(String shortName, String typeName, String diName) {
				test = true;
				this.shortName = shortName;
				this.typeName = typeName;
				this.diName = diName;
			}

			public void alias(String name, Class type, Class defaultImplementation) {
				if (test) {
					assertEquals(shortName, name);
					assertEquals(typeName, type.getName());
					assertEquals(diName, defaultImplementation.getName());
				}
			}

			public void alias(String name, Class type) {
				if (test) {
					assertEquals(shortName, name);
					assertEquals(typeName, type.getName());
				}
			}
		}

		// TEST1: test setting class alias with method alias(String,Class,Class)

		// create new XStream
		stream = new XStreamExt();
		// set expected values
		((XStreamExt) stream).init("testAlias", "java.util.List", "java.util.ArrayList");

		// create classAlias
		ClassAlias classAlias = new ClassAlias();
		classAlias.setName("testAlias");
		classAlias.setType("java.util.List");
		classAlias.setDefaultImplementation("java.util.ArrayList");

		// add it to the list of aliases
		List classAliases = new ArrayList();
		classAliases.add(classAlias);

		// create configuration object
		config = new XStreamConfiguration();
		// set list of classAliases
		config.setClassAliases(classAliases);

		// create factory
		factory = new XStreamFactory();
		// set config object
		factory.setConfig(config);
		// call set-up method for XStream
		factory.setUpXStream(stream);

		// TEST2: test setting class alias with method alias(String,Class)
		classAlias.setDefaultImplementation(null);
		factory.setUpXStream(stream);

		// TEST3: ClassNotFoundException for 'defaultImplementation' parameter
		classAlias.setDefaultImplementation("test.some.nonexisting.ClassName");
		// call set-up method for XStream - BatchEnvironmentException is
		// expected
		try {
			factory.setUpXStream(stream);
			fail("BatchEnvironmentException was expected");
		}
		catch (BatchEnvironmentException bee) {
			assertTrue(bee.getCause() instanceof ClassNotFoundException);
		}

		// TEST4: ClassNotFoundException for 'type' parameter
		classAlias.setDefaultImplementation(null);
		classAlias.setType("test.some.nonexisting.ClassName");
		// call set-up method for XStream - BatchEnvironmentException is
		// expected
		try {
			factory.setUpXStream(stream);
			fail("BatchEnvironmentException was expected");
		}
		catch (BatchEnvironmentException bee) {
			assertTrue(bee.getCause() instanceof ClassNotFoundException);
		}

	}

	public void testSetTypeAliases() {

		// override tested methods
		class XStreamExt extends XStream {

			private String shortName;

			private String typeName;

			private boolean test;

			public void init(String shortName, String typeName) {
				test = true;
				this.shortName = shortName;
				this.typeName = typeName;
			}

			public void aliasType(String name, Class type) {
				if (test) {
					assertEquals(shortName, name);
					assertEquals(typeName, type.getName());
				}
			}
		}

		// TEST1: test setting type alias with method alias(String,Class)

		// create new XStream
		stream = new XStreamExt();
		// set expected values
		((XStreamExt) stream).init("testAlias", "java.util.List");

		// create classAlias
		TypeAlias typeAlias = new TypeAlias();
		typeAlias.setName("testAlias");
		typeAlias.setType("java.util.List");

		// add it to the list of aliases
		List typeAliases = new ArrayList();
		typeAliases.add(typeAlias);

		// create configuration object
		config = new XStreamConfiguration();
		// set list of classAliases
		config.setTypeAliases(typeAliases);

		// create factory
		factory = new XStreamFactory();
		// set config object
		factory.setConfig(config);
		// call set-up method for XStream
		factory.setUpXStream(stream);

		// TEST2: ClassNotFoundException for 'type' parameter
		typeAlias.setType("test.some.nonexisting.ClassName");
		// call set-up method for XStream - BatchEnvironmentException is
		// expected
		try {
			factory.setUpXStream(stream);
			fail("BatchEnvironmentException was expected");
		}
		catch (BatchEnvironmentException bee) {
			assertTrue(bee.getCause() instanceof ClassNotFoundException);
		}
	}

	public void testSetFieldAliases() {

		// override tested methods
		class XStreamExt extends XStream {

			private String aliasName;

			private String typeName;

			private String fieldName;

			private boolean test;

			public void init(String aliasName, String typeName, String fieldName) {
				test = true;
				this.aliasName = aliasName;
				this.typeName = typeName;
				this.fieldName = fieldName;
			}

			public void aliasField(String aliasName, Class type, String fieldName) {
				if (test) {
					assertEquals(this.aliasName, aliasName);
					assertEquals(typeName, type.getName());
					assertEquals(this.fieldName, fieldName);
				}
			}
		}

		// TEST1: test setting type alias with method alias(String,Class)

		// create new XStream
		stream = new XStreamExt();
		// set expected values
		((XStreamExt) stream).init("testAlias", "java.util.List", "list");

		// create classAlias
		FieldAlias fieldAlias = new FieldAlias();
		fieldAlias.setAliasName("testAlias");
		fieldAlias.setType("java.util.List");
		fieldAlias.setFieldName("list");

		// add it to the list of aliases
		List fieldAliases = new ArrayList();
		fieldAliases.add(fieldAlias);

		// create configuration object
		config = new XStreamConfiguration();
		// set list of classAliases
		config.setFieldAliases(fieldAliases);

		// create factory
		factory = new XStreamFactory();
		// set config object
		factory.setConfig(config);
		// call set-up method for XStream
		factory.setUpXStream(stream);

		// TEST2: ClassNotFoundException for 'type' parameter
		fieldAlias.setType("test.some.nonexisting.ClassName");
		// call set-up method for XStream - BatchEnvironmentException is
		// expected
		try {
			factory.setUpXStream(stream);
			fail("BatchEnvironmentException was expected");
		}
		catch (BatchEnvironmentException bee) {
			assertTrue(bee.getCause() instanceof ClassNotFoundException);
		}
	}

	public void testSetAttributeAliases() {

		// override tested methods
		class XStreamExt extends XStream {

			private String alias;

			private String attributeName;

			private boolean test = false;

			public void init(String alias, String attributeName) {
				test = true;
				this.alias = alias;
				this.attributeName = attributeName;
			}

			public void aliasAttribute(String alias, String attributeName) {
				if (test) {
					assertEquals(this.alias, alias);
					assertEquals(this.attributeName, attributeName);
				}
			}
		}

		// TEST1: test adding attribute aliases

		// create new XStream
		stream = new XStreamExt();
		// set expected values
		((XStreamExt) stream).init("alias", "attribute");

		AttributeAlias alias = new AttributeAlias();
		alias.setAlias("alias");
		alias.setAttributeName("attribute");

		List aliases = new ArrayList();
		aliases.add(alias);

		// create configuration object
		config = new XStreamConfiguration();
		// set list of classAliases
		config.setAttributeAliases(aliases);

		// create factory
		factory = new XStreamFactory();
		// set config object
		factory.setConfig(config);
		// call set-up method for XStream
		factory.setUpXStream(stream);
	}

	public void testSetAttributes() {

		// override tested methods
		class XStreamExt extends XStream {

			private String type;

			private String fieldName;

			private boolean test = false;

			public void init(String type, String fieldName) {
				test = true;
				this.type = type;
				this.fieldName = fieldName;
			}

			public void useAttributeFor(Class type) {
				if (test) {
					assertEquals(this.type, type.getName());
				}
			}

			public void useAttributeFor(String fieldName, Class type) {
				if (test) {
					assertEquals(this.fieldName, fieldName);
					assertEquals(this.type, type.getName());
				}
			}
		}

		// TEST1: test adding attribute properties with
		// useAttributeFor(String,Class)

		// create new XStream
		stream = new XStreamExt();
		// set expected values
		((XStreamExt) stream).init("java.util.List", "fieldName");

		AttributeProperties props = new AttributeProperties();
		props.setFieldName("fieldName");
		props.setType("java.util.List");

		List properties = new ArrayList();
		properties.add(props);

		// create configuration object
		config = new XStreamConfiguration();
		// set list of classAliases
		config.setAttributes(properties);

		// create factory
		factory = new XStreamFactory();
		// set config object
		factory.setConfig(config);
		// call set-up method for XStream
		factory.setUpXStream(stream);

		// TEST2: test adding attribute properties with useAttributeFor(String)
		props.setFieldName(null);
		factory.setUpXStream(stream);

		// TEST3: ClassNotFoundException for 'type' parameter
		props.setType("test.some.nonexisting.ClassName");
		// call set-up method for XStream - BatchEnvironmentException is
		// expected
		try {
			factory.setUpXStream(stream);
			fail("BatchEnvironmentException was expected");
		}
		catch (BatchEnvironmentException bee) {
			assertTrue(bee.getCause() instanceof ClassNotFoundException);
		}
	}
	
// TODO different results for JDK1.4 and JDK1.5
//	public void testRegisterConverters() {
//
//		// override tested methods
//		class XStreamExt extends XStream {
//
//			private String className;
//
//			private int priority;
//
//			private boolean test = false;
//
//			public void init(String className, int priority) {
//				test = true;
//				this.className = className;
//				this.priority = priority;
//			}
//
//			public void registerConverter(Converter converter, int priority) {
//				if (test) {
//					assertEquals(className, converter.getClass().getName());
//					assertEquals(this.priority, priority);
//				}
//			}
//
//			public void registerConverter(SingleValueConverter converter, int priority) {
//				if (test) {
//					assertEquals(className, converter.getClass().getName());
//					assertEquals(this.priority, priority);
//				}
//			}
//		}
//
//		// TEST1: test registering single value converter
//
//		// create new XStream
//		stream = new XStreamExt();
//		// set expected values
//		((XStreamExt) stream).init("com.thoughtworks.xstream.converters.basic.FloatConverter", 10);
//
//		ConverterProperties cp = new ConverterProperties();
//		cp.setConverterClassName("com.thoughtworks.xstream.converters.basic.FloatConverter");
//		cp.setPriority(10);
//
//		List converters = new ArrayList();
//		converters.add(cp);
//
//		// create configuration object
//		config = new XStreamConfiguration();
//		// set list of classAliases
//		config.setConverters(converters);
//
//		// create factory
//		factory = new XStreamFactory();
//		// set config object
//		factory.setConfig(config);
//		// call set-up method for XStream
//		factory.setUpXStream(stream);
//
//		// TEST2: test registering converter
//		cp.setConverterClassName("com.thoughtworks.xstream.converters.basic.NullConverter");
//		((XStreamExt) stream).init("com.thoughtworks.xstream.converters.basic.NullConverter", 10);
//		factory.setUpXStream(stream);
//
//		// TEST3: BatchEnviromentException due to invalid type (not assignable
//		// to SingleValueConverter or Converter)
//		cp.setConverterClassName("java.util.List");
//		try {
//			factory.setUpXStream(stream);
//			fail("BatchEnvironmentException was expected");
//		}
//		catch (BatchEnvironmentException bee) {
//			assertNull(bee.getCause());
//		}
//
//		// TEST4: ClassNotFoundException
//		cp.setConverterClassName("test.some.nonexisting.ClassName");
//		try {
//			factory.setUpXStream(stream);
//			fail("BatchEnvironmentException was expected");
//		}
//		catch (BatchEnvironmentException bee) {
//			assertTrue(bee.getCause() instanceof ClassNotFoundException);
//		}
//
//		// TEST5: InstantiationException
//		// set interface as className
//		cp.setConverterClassName("com.thoughtworks.xstream.converters.Converter");
//		try {
//			factory.setUpXStream(stream);
//			fail("BatchEnvironmentException was expected");
//		}
//		catch (BatchEnvironmentException bee) {
//			assertTrue(bee.getCause() instanceof InstantiationException);
//		}
//	}

	public void testSetMode() {

		// override tested methods
		class XStreamExt extends XStream {

			private int mode;

			private boolean test = false;

			public void init(int mode) {
				test = true;
				this.mode = mode;
			}

			public void setMode(int mode) {
				if (test) {
					assertEquals(this.mode, mode);
				}
			}
		}

		// create new XStream
		stream = new XStreamExt();
		((XStreamExt) stream).init(1001);

		// create configuration object
		config = new XStreamConfiguration();
		// set list of classAliases
		config.setMode(1001);

		// create factory
		factory = new XStreamFactory();
		// set config object
		factory.setConfig(config);
		// call set-up method for XStream
		factory.setUpXStream(stream);
	}

	public void testAddImplicitCollections() {

		// override tested methods
		class XStreamExt extends XStream {

			private String ownerType;

			private String fieldName;

			private String itemFieldName;

			private String itemType;

			private boolean test = false;

			/*
			 * Set expected values
			 */
			public void init(String ownerType, String fieldName, String itemFieldName, String itemType) {
				test = true;
				this.ownerType = ownerType;
				this.fieldName = fieldName;
				this.itemFieldName = itemFieldName;
				this.itemType = itemType;
			}

			public void addImplicitCollection(Class ownerType, String fieldName, Class itemType) {
				if (test) {
					assertEquals(this.ownerType, ownerType.getName());
					assertEquals(this.fieldName, fieldName);
					assertEquals(this.itemType, itemType.getName());
				}
			}

			public void addImplicitCollection(Class ownerType, String fieldName, String itemFieldName, Class itemType) {
				if (test) {
					assertEquals(this.ownerType, ownerType.getName());
					assertEquals(this.fieldName, fieldName);
					assertEquals(this.itemFieldName, itemFieldName);
					assertEquals(this.itemType, itemType.getName());
				}
			}

			public void addImplicitCollection(Class ownerType, String fieldName) {
				if (test) {
					assertEquals(this.ownerType, ownerType.getName());
					assertEquals(this.fieldName, fieldName);
				}
			}
		}

		// create new XStream
		stream = new XStreamExt();
		((XStreamExt) stream).init("java.util.List", "fieldName", "itemFieldName", "java.util.Map");

		// TEST1: test adding implicit collection with
		// addImplicitCollection(Class, String)
		ImplicitCollection implicitCollection = new ImplicitCollection();
		implicitCollection.setOwnerType("java.util.List");
		implicitCollection.setFieldName("fieldName");

		List implicitCollections = new ArrayList();
		implicitCollections.add(implicitCollection);

		// create configuration object
		config = new XStreamConfiguration();
		// set list of classAliases
		config.setImplicitCollections(implicitCollections);

		// create factory
		factory = new XStreamFactory();
		// set config object
		factory.setConfig(config);
		// call set-up method for XStream
		factory.setUpXStream(stream);

		// TEST2: test adding implicit collection with
		// addImplicitCollection(Class, String, String)
		implicitCollection.setItemType("java.util.Map");
		factory.setUpXStream(stream);

		// TEST3: test adding implicit collection with
		// addImplicitCollection(Class, String, String, String)
		implicitCollection.setItemFieldName("itemFieldName");
		factory.setUpXStream(stream);

		// TEST4: ClassNotFoundException due to non-existing class name in
		// itemType
		implicitCollection.setItemType("test.some.nonexisting.ClassName");
		try {
			factory.setUpXStream(stream);
			fail("BatchEnvironmentException was expected");
		}
		catch (BatchEnvironmentException bee) {
			assertTrue(bee.getCause() instanceof ClassNotFoundException);
		}

		// TEST5: ClassNotFoundException due to non-existing class name in
		// ownerType
		implicitCollection.setOwnerType("test.some.nonexisting.ClassName");
		try {
			factory.setUpXStream(stream);
			fail("BatchEnvironmentException was expected");
		}
		catch (BatchEnvironmentException bee) {
			assertTrue(bee.getCause() instanceof ClassNotFoundException);
		}
	}

	public void testSetOmittedFields() {

		// override tested methods
		class XStreamExt extends XStream {

			private String type;

			private String fieldName;

			private boolean test = false;

			public void init(String type, String fieldName) {
				test = true;
				this.type = type;
				this.fieldName = fieldName;
			}

			public void omitField(Class type, String fieldName) {
				if (test) {
					assertEquals(this.type, type.getName());
					assertEquals(this.fieldName, fieldName);
				}
			}
		}

		// TEST1: test adding ommited fields

		// create new XStream
		stream = new XStreamExt();
		// set expected values
		((XStreamExt) stream).init("java.util.List", "fieldName");

		OmmitedField ommitedField = new OmmitedField();
		ommitedField.setType("java.util.List");
		ommitedField.setFieldName("fieldName");

		List ommitedFields = new ArrayList();
		ommitedFields.add(ommitedField);

		// create configuration object
		config = new XStreamConfiguration();
		// set list of classAliases
		config.setOmmitedFields(ommitedFields);

		// create factory
		factory = new XStreamFactory();
		// set config object
		factory.setConfig(config);
		// call set-up method for XStream
		factory.setUpXStream(stream);

		// TEST2: ClassNotFoundException
		ommitedField.setType("test.some.nonexisting.ClassName");
		try {
			factory.setUpXStream(stream);
			fail("BatchEnvironmentException was expected");
		}
		catch (BatchEnvironmentException bee) {
			assertTrue(bee.getCause() instanceof ClassNotFoundException);
		}
	}

	public void testAddImmutableTypes() {

		// override tested methods
		class XStreamExt extends XStream {

			private String type;

			private boolean test = false;

			public void init(String type) {
				test = true;
				this.type = type;

			}

			public void addImmutableType(Class type) {
				if (test) {
					assertEquals(this.type, type.getName());
				}
			}
		}

		// create new XStream
		stream = new XStreamExt();
		// set expected values
		((XStreamExt) stream).init("java.util.List");

		List immutableTypes = new ArrayList();
		immutableTypes.add("java.util.List");

		// create configuration object
		config = new XStreamConfiguration();
		// set list of classAliases
		config.setImmutableTypes(immutableTypes);

		// create factory
		factory = new XStreamFactory();
		// set config object
		factory.setConfig(config);
		// call set-up method for XStream
		factory.setUpXStream(stream);

		// TEST2: ClassNotFoundException
		immutableTypes.clear();
		immutableTypes.add("test.some.nonexisting.ClassName");
		try {
			factory.setUpXStream(stream);
			fail("BatchEnvironmentException was expected");
		}
		catch (BatchEnvironmentException bee) {
			assertTrue(bee.getCause() instanceof ClassNotFoundException);
		}
	}

	public void testWriteAndRead() throws IOException, ClassNotFoundException {

		// create file
		File file = File.createTempFile("test", ".xml");
		// create factory and set empty configuration
		XStreamFactory factory = new XStreamFactory();
		factory.setConfig(new XStreamConfiguration());

		// define test class
		class TestValueObject {
			String param1;

			int param2;

			Long param3;
		}

		TestValueObject valueObject = new TestValueObject();
		valueObject.param1 = "test";
		valueObject.param2 = 392;
		valueObject.param3 = new Long(632);

		// just a simple test for object output and input: write object to XML
		// and read it back

		ObjectOutput output = factory.createObjectOutput(new FileSystemResource(file), "UTF-8");
		output.writeObject(valueObject);
		output.close();

		ObjectInput input = factory.createObjectInput(new FileSystemResource(file), "UTF-8");
		Object result = input.readObject();
		input.close();
		file.delete();

		// is result instance of TestValueObject?
		assertTrue(result instanceof TestValueObject);
		// is result equal to written object?
		assertEquals(valueObject.param1, ((TestValueObject) result).param1);
		assertEquals(valueObject.param2, ((TestValueObject) result).param2);
		assertEquals(valueObject.param3, ((TestValueObject) result).param3);
	}
}
