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

import com.thoughtworks.xstream.XStream;

/**
 * Value object, which holds configuration for XStream.
 * 
 * @author peter.zozom
 * @author Dave Syer
 * 
 * @see XStreamConfigurationFactoryBean
 * @see Mapping
 * @see ClassAlias
 * @see TypeAlias
 * @see FieldAlias
 * @see AttributeAlias
 * @see AttributeProperties
 * @see ConverterProperties
 * @see XStream#setMode(int)
 * @see ImplicitCollection
 * @see OmmitedField
 * @see XStream#addImmutableType(Class)
 * @see DefaultImplementation
 */
public class XStreamConfiguration {

	private List mappings = null;

	private String rootElementName = null;

	private Map rootElementAttributes;

	private List classAliases = null;

	private List typeAliases = null;

	private List fieldAliases = null;

	private List attributeAliases = null;

	private List attributes = null;

	private List converters = null;

	private int mode = XStream.XPATH_RELATIVE_REFERENCES;

	private List implicitCollections = null;

	private List ommitedFields = null;

	private List immutableTypes = null;

	private List defaultImplementations = null;

	/**
	 * @return list of the {@link DefaultImplementation} objects
	 */
	public List getDefaultImplementations() {
		return defaultImplementations;
	}

	/**
	 * Set list of default implementations.
	 * @param defaultImplementations list of the {@link DefaultImplementation}
	 * objects
	 * @see DefaultImplementation
	 */
	public void setDefaultImplementations(List defaultImplementations) {
		this.defaultImplementations = defaultImplementations;
	}

	/**
	 * @return list of the immutable type names
	 */
	public List getImmutableTypes() {
		return immutableTypes;
	}

	/**
	 * Set list of immutable types.
	 * @param immutableTypes list of the immutable type names
	 * @see XStream#addImmutableType(Class)
	 */
	public void setImmutableTypes(List immutableTypes) {
		this.immutableTypes = immutableTypes;
	}

	/**
	 * @return list of the {@link AttributeAlias} objects
	 */
	public List getAttributeAliases() {
		return attributeAliases;
	}

	/**
	 * Set list of attribute aliases.
	 * @param attributeAliases list of the {@link AttributeAlias} objects
	 * @see AttributeAlias
	 */
	public void setAttributeAliases(List attributeAliases) {
		this.attributeAliases = attributeAliases;
	}

	/**
	 * @return list of the {@link AttributeProperties} objects
	 */
	public List getAttributes() {
		return attributes;
	}

	/**
	 * Set list of attribute properties.
	 * @param attributes list of the {@link AttributeProperties}
	 * objects
	 * @see AttributeProperties
	 */
	public void setAttributes(List attributes) {
		this.attributes = attributes;
	}

	/**
	 * @return the classAliases
	 */
	public List getClassAliases() {
		return classAliases;
	}

	/**
	 * Set list of class aliases.
	 * @param classAliases the classAliases to set
	 * @see ClassAlias
	 */
	public void setClassAliases(List classAliases) {
		this.classAliases = classAliases;
	}

	/**
	 * @return list of the {@link ConverterProperties} objects
	 */
	public List getConverters() {
		return converters;
	}

	/**
	 * Set list of custom converters.
	 * @param converters list of the {@link ConverterProperties} objects
	 * @see ConverterProperties
	 */
	public void setConverters(List converters) {
		this.converters = converters;
	}

	/**
	 * @return the fieldAliases
	 */
	public List getFieldAliases() {
		return fieldAliases;
	}

	/**
	 * Set list of field aliases.
	 * @param fieldAliases the list of fieldAliases to set
	 * @see FieldAlias
	 */
	public void setFieldAliases(List fieldAliases) {
		this.fieldAliases = fieldAliases;
	}

	/**
	 * @return list of the {@link ImplicitCollections} objects
	 */
	public List getImplicitCollections() {
		return implicitCollections;
	}

	/**
	 * Set list of implicit collection definitions.
	 * @param implicitCollections list of the {@link ImplicitCollections}
	 * objects
	 * @see ImplicitCollection
	 */
	public void setImplicitCollections(List implicitCollections) {
		this.implicitCollections = implicitCollections;
	}

	/**
	 * @return list of the {@link Mapping} objects
	 */
	public List getMappings() {
		return mappings;
	}

	/**
	 * Set list of "qualified tag name - to - class name" mappigs.
	 * @param mappings list of the {@link Mapping} objects
	 * @see Mapping
	 */
	public void setMappings(List mappings) {
		this.mappings = mappings;
	}

	/**
	 * @return the actual mode
	 */
	public int getMode() {
		return mode;
	}

	/**
	 * Set mode for dealing with duplicate references. If not provided, default
	 * value is used ({@link XStream#XPATH_RELATIVE_REFERENCES}).
	 * @param mode the mode to set
	 * @see XStream#setMode(int)
	 */
	public void setMode(int mode) {
		this.mode = mode;
	}

	/**
	 * @return list of the {@link OmmitedFields} objects
	 */
	public List getOmmitedFields() {
		return ommitedFields;
	}

	/**
	 * Set list of ommited fields.
	 * @param ommitedFields list of the {@link OmmitedFields} objects
	 * @see OmmitedField
	 */
	public void setOmmitedFields(List ommitedFields) {
		this.ommitedFields = ommitedFields;
	}

	/**
	 * @return the root element attributes
	 */
	public Map getRootElementAttributes() {
		return rootElementAttributes;
	}

	/**
	 * Set attributes of root element. Each Map entry has key = "attribute name"
	 * and value = "attribute value".
	 * @param rootElementAttributes map of the root element attributes
	 */
	public void setRootElementAttributes(Map rootElementAttributes) {
		this.rootElementAttributes = rootElementAttributes;
	}

	/**
	 * @return the root element name
	 */
	public String getRootElementName() {
		return rootElementName;
	}

	/**
	 * Set name of the root element. Valid only for writing to XML.
	 * @param rootElementName the root element name
	 */
	public void setRootElementName(String rootElementName) {
		this.rootElementName = rootElementName;
	}

	/**
	 * @return the list of the {@link TypeAlias} objects
	 */
	public List getTypeAliases() {
		return typeAliases;
	}

	/**
	 * Set list of type aliases.
	 * @param typeAliases list of the {@link TypeAlias} objects
	 * @see TypeAlias
	 */
	public void setTypeAliases(List typeAliases) {
		this.typeAliases = typeAliases;
	}

}
