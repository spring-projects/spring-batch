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

import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.batch.io.exception.BatchEnvironmentException;
import org.springframework.core.io.ClassPathResource;

/**
 * Integration tests for XStreamConfigurationFactory.
 * @author peter.zozom
 * @author Dave Syer
 */
public class XStreamConfigurationFactoryBeanIntegrationTests extends TestCase {

	XStreamConfigurationFactoryBean factory;

	public void setUp() {
		factory = new XStreamConfigurationFactoryBean();
	}

	/**
	 * Test getXStreamCofiguration() method.
	 * @throws Exception 
	 */
	public void testGetXStreamConfiguration() throws Exception {

		// set config file
		factory.setConfigFile(new ClassPathResource("xstream-config-test.xml", getClass()));
		// get XStreamConfiguration
		factory.afterPropertiesSet();
		XStreamConfiguration config = (XStreamConfiguration) factory.getObject();

		// test mode
		assertEquals(1003, config.getMode());

		// test root element name
		assertEquals("root_test", config.getRootElementName());

		// test root element attributes
		Map rea = config.getRootElementAttributes();
		assertNotNull(rea);
		assertEquals(2, rea.size());
		assertEquals("root-elementAttr_value1", rea.get("root-elementAttr_key1"));
		assertEquals("root-elementAttr_value2", rea.get("root-elementAttr_key2"));

		// test class aliases
		List aliases = config.getClassAliases();
		assertNotNull(aliases);
		assertEquals(2, aliases.size());

		ClassAlias classAlias = (ClassAlias) aliases.get(0);
		assertEquals("class-alias_name1", classAlias.getName());
		assertEquals("class-alias_type1", classAlias.getType());
		assertEquals("class-alias_di1", classAlias.getDefaultImplementation());

		classAlias = (ClassAlias) aliases.get(1);
		assertEquals("class-alias_name2", classAlias.getName());
		assertEquals("class-alias_type2", classAlias.getType());
		assertEquals("class-alias_di2", classAlias.getDefaultImplementation());

		// test type aliases
		aliases = config.getTypeAliases();
		assertNotNull(aliases);
		assertEquals(2, aliases.size());

		TypeAlias typeAlias = (TypeAlias) aliases.get(0);
		assertEquals("type-alias_name1", typeAlias.getName());
		assertEquals("type-alias_type1", typeAlias.getType());

		typeAlias = (TypeAlias) aliases.get(1);
		assertEquals("type-alias_name2", typeAlias.getName());
		assertEquals("type-alias_type2", typeAlias.getType());

		// test field aliases
		aliases = config.getFieldAliases();
		assertNotNull(aliases);
		assertEquals(2, aliases.size());

		FieldAlias fieldAlias = (FieldAlias) aliases.get(0);
		assertEquals("field-alias_name1", fieldAlias.getAliasName());
		assertEquals("field-alias_type1", fieldAlias.getType());
		assertEquals("field1", fieldAlias.getFieldName());

		fieldAlias = (FieldAlias) aliases.get(1);
		assertEquals("field-alias_name2", fieldAlias.getAliasName());
		assertEquals("field-alias_type2", fieldAlias.getType());
		assertEquals("field2", fieldAlias.getFieldName());

		// test attribute alias
		aliases = config.getAttributeAliases();
		assertNotNull(aliases);
		assertEquals(2, aliases.size());

		AttributeAlias attributeAlias = (AttributeAlias) aliases.get(0);
		assertEquals("attribute-alias_name1", attributeAlias.getAttributeName());
		assertEquals("attribute-alias_alias1", attributeAlias.getAlias());

		attributeAlias = (AttributeAlias) aliases.get(1);
		assertEquals("attribute-alias_name2", attributeAlias.getAttributeName());
		assertEquals("attribute-alias_alias2", attributeAlias.getAlias());

		// test attribute properties
		List properties = config.getAttributes();
		assertNotNull(properties);
		assertEquals(2, properties.size());

		AttributeProperties attributeProperties = (AttributeProperties) properties.get(0);
		assertEquals("attribute-properties_type1", attributeProperties.getType());
		assertEquals("attribute-properties_field1", attributeProperties.getFieldName());

		attributeProperties = (AttributeProperties) properties.get(1);
		assertEquals("attribute-properties_type2", attributeProperties.getType());
		assertEquals("attribute-properties_field2", attributeProperties.getFieldName());

		// test converters
		properties = config.getConverters();
		assertNotNull(properties);
		assertEquals(2, properties.size());

		ConverterProperties converterProperties = (ConverterProperties) properties.get(0);
		assertEquals("converter.class-name1", converterProperties.getClassName());
		assertEquals(-50, converterProperties.getPriority());

		converterProperties = (ConverterProperties) properties.get(1);
		assertEquals("converter.class-name2", converterProperties.getClassName());
		assertEquals(750, converterProperties.getPriority());

		// test implicit collections
		List collections = config.getImplicitCollections();
		assertNotNull(collections);
		assertEquals(2, collections.size());

		ImplicitCollection implicitCollection = (ImplicitCollection) collections.get(0);
		assertEquals("ic_owner-type1", implicitCollection.getOwnerType());
		assertEquals("ic_field-name1", implicitCollection.getFieldName());
		assertEquals("ic_itemField-name1", implicitCollection.getItemFieldName());
		assertEquals("ic_item-type1", implicitCollection.getItemType());

		implicitCollection = (ImplicitCollection) collections.get(1);
		assertEquals("ic_owner-type2", implicitCollection.getOwnerType());
		assertEquals("ic_field-name2", implicitCollection.getFieldName());
		assertNull(implicitCollection.getItemFieldName());
		assertEquals("ic_item-type2", implicitCollection.getItemType());

		// test ommited fields
		List fields = config.getOmmitedFields();
		assertNotNull(fields);
		assertEquals(2, fields.size());

		OmmitedField ommitedField = (OmmitedField) fields.get(0);
		assertEquals("ommited-field_type1", ommitedField.getType());
		assertEquals("ommited-field_field1", ommitedField.getFieldName());

		ommitedField = (OmmitedField) fields.get(1);
		assertEquals("ommited-field_type2", ommitedField.getType());
		assertEquals("ommited-field_field2", ommitedField.getFieldName());

		// test immutable types
		List types = config.getImmutableTypes();
		assertNotNull(types);
		assertEquals(2, types.size());
		assertEquals("immutable-type1", types.get(0));
		assertEquals("immutable-type2", types.get(1));

		// test default implementations
		List implementations = config.getDefaultImplementations();
		assertNotNull(implementations);
		assertEquals(2, implementations.size());

		DefaultImplementation defaultImplementation = (DefaultImplementation) implementations.get(0);
		assertEquals("default-implementation1", defaultImplementation.getDefaultImpl());
		assertEquals("type1", defaultImplementation.getType());

		defaultImplementation = (DefaultImplementation) implementations.get(1);
		assertEquals("default-implementation2", defaultImplementation.getDefaultImpl());
		assertEquals("type2", defaultImplementation.getType());

		// test mappings
		List mappings = config.getMappings();
		assertNotNull(mappings);
		assertEquals(2, mappings.size());

		Mapping mapping = (Mapping) mappings.get(0);
		assertEquals("uri1", mapping.getNamespaceURI());
		assertEquals("localpart1", mapping.getLocalPart());
		assertEquals("prefix1", mapping.getPrefix());
		assertEquals("classname1", mapping.getClassName());

		mapping = (Mapping) mappings.get(1);
		assertEquals("uri2", mapping.getNamespaceURI());
		assertEquals("localpart2", mapping.getLocalPart());
		assertEquals("prefix2", mapping.getPrefix());
		assertEquals("classname2", mapping.getClassName());

	}

	/**
	 * Test getXStreamConfiguration with non-existing config file.
	 * @throws Exception 
	 */
	public void testNonExistingConfigFile() throws Exception {

		// set config file to non-existing file
		factory.setConfigFile(new ClassPathResource("nonexisting-xstream-config-file.xml"));

		// try to get XStreamConfiguration
		try {
			factory.afterPropertiesSet();
			fail("BatchEnvironmentException was expected");
		}
		catch (BatchEnvironmentException bee) {
			assertTrue(true);
		}
	}
}
