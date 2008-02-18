package org.springframework.batch.execution.configuration;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

class JobBeanDefinitionParser implements BeanDefinitionParser {
	
	private static final String JOB = "job";
	
	private static final String CHUNKING_STEP = "chunking-step";
	
	private static final String TASKLET_STEP ="tasklet-step";

	public BeanDefinition parse(Element element, ParserContext parserContext) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

}
