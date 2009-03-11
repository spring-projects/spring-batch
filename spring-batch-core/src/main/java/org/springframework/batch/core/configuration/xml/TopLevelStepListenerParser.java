package org.springframework.batch.core.configuration.xml;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class TopLevelStepListenerParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {

		StepListenerParser stepListenerParser = new StepListenerParser();
		return stepListenerParser.parse(element, parserContext);

	}

}
