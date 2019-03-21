/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import java.util.List;

import org.springframework.batch.core.step.item.ForceRollbackForWriteSkipException;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

public class ExceptionElementParser {

	public ManagedMap<TypedStringValue, Boolean> parse(Element element, ParserContext parserContext, String exceptionListName) {
		List<Element> children = DomUtils.getChildElementsByTagName(element, exceptionListName);
		if (children.size() == 1) {
			ManagedMap<TypedStringValue, Boolean> map = new ManagedMap<>();
			Element exceptionClassesElement = children.get(0);
			addExceptionClasses("include", true, exceptionClassesElement, map, parserContext);
			addExceptionClasses("exclude", false, exceptionClassesElement, map, parserContext);
			map.put(new TypedStringValue(ForceRollbackForWriteSkipException.class.getName(), Class.class), true);
			return map;
		}
		else if (children.size() > 1) {
			parserContext.getReaderContext().error(
					"The <" + exceptionListName + "/> element may not appear more than once in a single <"
							+ element.getNodeName() + "/>.", element);
		}
		return null;
	}

	private void addExceptionClasses(String elementName, boolean include, Element exceptionClassesElement,
			ManagedMap<TypedStringValue, Boolean> map, ParserContext parserContext) {
		for (Element child : DomUtils.getChildElementsByTagName(exceptionClassesElement, elementName)) {
			String className = child.getAttribute("class");
			map.put(new TypedStringValue(className, Class.class), include);
		}
	}
}
