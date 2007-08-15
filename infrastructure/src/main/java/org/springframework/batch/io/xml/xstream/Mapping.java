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

/**
 * Represents a mapping of qualified tag names to Java class names allowing
 * class aliases and namespace aware mappings of qualified tag names to class
 * names.
 * @author peter.zozom
 * @see javax.xml.namespace.QName
 */
public class Mapping {

	private String namespaceURI;

	private String localPart;

	private String prefix;

	private String className;

	/**
	 * @return class name
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Set Java type which will be mapped.
	 * @param className type name to be mapped
	 */
	public void setClassName(String className) {
		this.className = className;
	}

	/**
	 * @return local part of the qualified name
	 */
	public String getLocalPart() {
		return localPart;
	}

	/**
	 * Set local part of the qualified name.
	 * @param localPart local part of the qualified name
	 * @see javax.xml.namespace.QName
	 */
	public void setLocalPart(String localPart) {
		this.localPart = localPart;
	}

	/**
	 * @return namespace URI of the qualified name
	 */
	public String getNamespaceURI() {
		return namespaceURI;
	}

	/**
	 * Set namespace URI of the qualified name.
	 * @param namespaceURI namespace URI of the qualified name
	 * @see javax.xml.namespace.QName
	 */
	public void setNamespaceURI(String namespaceURI) {
		this.namespaceURI = namespaceURI;
	}

	/**
	 * @return prefix of the qualified name
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Set prefix of the qualified name.
	 * @param prefix prefix of the qualified name
	 * @see javax.xml.namespace.QName
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
}
