package org.springframework.batch.core.configuration.xml;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.listener.AbstractListenerFactoryBean;
import org.springframework.batch.core.listener.ListenerMetaData;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * @author Dan Garrette
 * @since 2.0
 * @see StepListenerParser
 * @see JobExecutionListenerParser
 */
public abstract class AbstractListenerParser {

	private static final String ID_ATTR = "id";
	
	private static final String REF_ATTR = "ref";
	
	private static final String CLASS_ATTR = "class";

	public AbstractBeanDefinition parse(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(getBeanClass());
		doParse(element, parserContext, builder);
		return builder.getBeanDefinition();
	}

	@SuppressWarnings("unchecked")
	public void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String id = element.getAttribute(ID_ATTR);
		String listenerRef = element.getAttribute(REF_ATTR);
		String className = element.getAttribute(CLASS_ATTR);
		checkListenerElementAttributes(parserContext, element, id, listenerRef, className);

		if (StringUtils.hasText(listenerRef)) {
			builder.addPropertyReference("delegate", listenerRef);
		}
		else if (StringUtils.hasText(className)) {
			RootBeanDefinition beanDef = new RootBeanDefinition(className, null, null);
			builder.addPropertyValue("delegate", beanDef);
		}
		else {
			parserContext.getReaderContext().error(
					"Neither '" + REF_ATTR + "' or '" + CLASS_ATTR + "' specified for <" + element.getTagName()
							+ "> element", element);
		}

		ManagedMap metaDataMap = new ManagedMap();
		for (String metaDataPropertyName : getMethodNameAttributes()) {
			String listenerMethod = element.getAttribute(metaDataPropertyName);
			if (StringUtils.hasText(listenerMethod)) {
				metaDataMap.put(metaDataPropertyName, listenerMethod);
			}
		}
		builder.addPropertyValue("metaDataMap", metaDataMap);
	}

	private void checkListenerElementAttributes(ParserContext parserContext, Element element, String id,
			String listenerRef, String className) {
		if (StringUtils.hasText(className) && StringUtils.hasText(listenerRef)) {
			NamedNodeMap attributeNodes = element.getAttributes();
			StringBuilder attributes = new StringBuilder();
			for (int i = 0; i < attributeNodes.getLength(); i++) {
				if (i > 0) {
					attributes.append(" ");
				}
				attributes.append(attributeNodes.item(i));
			}
			parserContext.getReaderContext().error(
					"Either '" + REF_ATTR + "' or '" + CLASS_ATTR + "' may be specified, but not both; <"
							+ element.getTagName() + "> element specified with attributes: " + attributes, element);
		}
	}

	private List<String> getMethodNameAttributes() {
		List<String> methodNameAttributes = new ArrayList<String>();
		for (ListenerMetaData metaData : getMetaDataValues()) {
			methodNameAttributes.add(metaData.getMethodName());
		}
		return methodNameAttributes;
	}

	protected abstract Class<? extends AbstractListenerFactoryBean> getBeanClass();

	protected abstract ListenerMetaData[] getMetaDataValues();

}