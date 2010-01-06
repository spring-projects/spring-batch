package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.listener.AbstractListenerFactoryBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Parse &lt;step-listener/&gt; elements in the batch namespace.
 * 
 * @author Dan Garrette
 * @since 2.0
 */
public class TopLevelStepListenerParser extends AbstractSingleBeanDefinitionParser {

	private static final StepListenerParser stepListenerParser = new StepListenerParser();

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		CoreNamespaceUtils.autoregisterBeansForNamespace(parserContext, element);
		stepListenerParser.doParse(element, parserContext, builder);
	}

	@Override
	protected Class<? extends AbstractListenerFactoryBean> getBeanClass(Element element) {
		return stepListenerParser.getBeanClass();
	}

}
