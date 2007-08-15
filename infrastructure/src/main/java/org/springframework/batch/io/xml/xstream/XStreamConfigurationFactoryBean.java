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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.io.exception.BatchEnvironmentException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.Resource;

import com.thoughtworks.xstream.XStream;

/**
 * Factory creates {@link XStreamConfiguration} object, which hold XStream's
 * configuration settings. These settings are read from provided configuration
 * XML file.
 * 
 * @author peter.zozom
 * @author Dave Syer
 */
public class XStreamConfigurationFactoryBean extends AbstractFactoryBean {

	private Log log = LogFactory.getLog(XStreamConfigurationFactoryBean.class);

	private Resource resource;
	
	/**
	 * Creates {@link XStreamConfiguration} object from XStream's config file.
	 * @return XStream's configuration settings.
	 */
	private XStreamConfiguration getXStreamConfiguration() {

		XStream stream = new XStream();
		setConfigAliases(stream);

		XStreamConfiguration xsc;
		try {
			InputStream is = resource.getInputStream();
			xsc = (XStreamConfiguration) stream.fromXML(is);
			is.close();
		}
		catch (IOException ioe) {
			log.debug(ioe);
			throw new BatchEnvironmentException("Could not read XStream mapping file.", ioe);
		}

		return xsc;
	}

	/*
	 * Set aliases necessary for parsing configuration file in xstream format.
	 */
	private void setConfigAliases(XStream stream) {

		stream.aliasField("root-element-name", XStreamConfiguration.class, "rootElementName");
		stream.aliasField("root-element-attributes", XStreamConfiguration.class, "rootElementAttributes");

		stream.alias("class-alias", ClassAlias.class);
		stream.aliasField("default-implementation", ClassAlias.class, "defaultImplementation");
		stream.aliasField("class-aliases", XStreamConfiguration.class, "classAliases");

		stream.alias("type-alias", TypeAlias.class);
		stream.aliasField("type-aliases", XStreamConfiguration.class, "typeAliases");

		stream.alias("field-alias", FieldAlias.class);
		stream.aliasField("alias-name", FieldAlias.class, "aliasName");
		stream.aliasField("field-name", FieldAlias.class, "fieldName");
		stream.aliasField("field-aliases", XStreamConfiguration.class, "fieldAliases");

		stream.alias("attribute-alias", AttributeAlias.class);
		stream.aliasField("attribute-name", AttributeAlias.class, "attributeName");
		stream.aliasField("attribute-aliases", XStreamConfiguration.class, "attributeAliases");
		
		stream.alias("attribute-properties", AttributeProperties.class);
		stream.aliasField("field-name", AttributeProperties.class, "fieldName");
		stream.aliasField("attributes", XStreamConfiguration.class, "attributes");
		
		stream.alias("converter-properties", ConverterProperties.class);
		stream.aliasField("class-name", ConverterProperties.class, "className");
		
		stream.alias("implicit-collection", ImplicitCollection.class);
		stream.aliasField("owner-type", ImplicitCollection.class, "ownerType");
		stream.aliasField("item-type", ImplicitCollection.class, "itemType");
		stream.aliasField("field-name", ImplicitCollection.class, "fieldName");
		stream.aliasField("item-field-name", ImplicitCollection.class, "itemFieldName");
		stream.aliasField("implicit-collections", XStreamConfiguration.class, "implicitCollections");
		
		stream.alias("ommited-field", OmmitedField.class);
		stream.aliasField("field-name", OmmitedField.class, "fieldName");
		stream.aliasField("ommited-fields", XStreamConfiguration.class, "ommitedFields");
		
		stream.alias("default-implementation", DefaultImplementation.class);
		stream.aliasField("default-impl", DefaultImplementation.class, "defaultImpl");
		stream.aliasField("default-implementations", XStreamConfiguration.class, "defaultImplementations");
		
		stream.aliasField("immutable-types", XStreamConfiguration.class, "immutableTypes");
		
		stream.alias("attribute", Entry.class);
		stream.alias("mapping", Mapping.class);
		stream.aliasField("class-name", Mapping.class, "className");
		stream.alias("configuration", XStreamConfiguration.class);
		stream.alias("key", java.lang.String.class);
		stream.alias("value", java.lang.String.class);
		stream.alias("type", java.lang.String.class);
	}

	/**
	 * Set the filename of the configuration XML file.
	 * @param configFile resource for reading configuration
	 */
	public void setConfigFile(Resource resource) {
		this.resource = resource;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.config.AbstractFactoryBean#createInstance()
	 */
	protected Object createInstance() throws Exception {
		return getXStreamConfiguration();
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.config.AbstractFactoryBean#getObjectType()
	 */
	public Class getObjectType() {
		return XStreamConfiguration.class;
	}
}
