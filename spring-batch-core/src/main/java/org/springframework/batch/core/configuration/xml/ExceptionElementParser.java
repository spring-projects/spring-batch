package org.springframework.batch.core.configuration.xml;

import java.util.List;

import org.springframework.batch.core.step.item.ForceRollbackForWriteSkipException;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

public class ExceptionElementParser {

	@SuppressWarnings("unchecked")
	public ManagedMap parse(Element element, ParserContext parserContext, String exceptionListName) {
		List<Element> children = DomUtils.getChildElementsByTagName(element, exceptionListName);
		if (children.size() == 1) {
			ManagedMap map = new ManagedMap();
			Element exceptionClassesElement = children.get(0);
			addExceptionClasses("include", true, exceptionClassesElement, map, parserContext);
			addExceptionClasses("exclude", false, exceptionClassesElement, map, parserContext);
			map.put(ForceRollbackForWriteSkipException.class, true);
			return map;
		}
		else if (children.size() > 1) {
			parserContext.getReaderContext().error(
					"The <" + exceptionListName + "/> element may not appear more than once in a single <"
							+ element.getNodeName() + "/>.", element);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private void addExceptionClasses(String elementName, boolean include, Element exceptionClassesElement,
			ManagedMap map, ParserContext parserContext) {
		for (Element child : DomUtils.getChildElementsByTagName(exceptionClassesElement, elementName)) {
			String className = child.getAttribute("class");
			map.put(new TypedStringValue(className, Class.class), include);
		}
	}
}
