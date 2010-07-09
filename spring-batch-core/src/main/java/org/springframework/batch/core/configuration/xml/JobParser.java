/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parser for the lt;job/gt; element in the Batch namespace. Sets up and returns
 * a bean definition for a {@link org.springframework.batch.core.Job}.
 * 
 * @author Dave Syer
 * 
 */
public class JobParser extends AbstractSingleBeanDefinitionParser {

	private static final String MERGE_ATTR = "merge";

	private static final String REF_ATTR = "ref";

	private static final String BEAN_ELE = "bean";

	private static final String REF_ELE = "ref";

	private static final JobExecutionListenerParser jobListenerParser = new JobExecutionListenerParser();

	@Override
	protected Class<JobParserJobFactoryBean> getBeanClass(Element element) {
		return JobParserJobFactoryBean.class;
	}

	/**
	 * Create a bean definition for a
	 * {@link org.springframework.batch.core.job.flow.FlowJob}. Nested step
	 * elements are delegated to an {@link InlineStepParser}.
	 * 
	 * @see AbstractSingleBeanDefinitionParser#doParse(Element, ParserContext,
	 * BeanDefinitionBuilder)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

		if (!CoreNamespaceUtils.namespaceMatchesVersion(element)) {
			parserContext.getReaderContext().error(
					"You cannot use spring-batch-2.0.xsd with Spring Batch 2.1.  Please upgrade your schema declarations "
							+ "(or use the spring-batch.xsd alias if you are feeling lucky).", element);
			return;
		}

		CoreNamespaceUtils.autoregisterBeansForNamespace(parserContext, parserContext.extractSource(element));

		String jobName = element.getAttribute("id");
		builder.addConstructorArgValue(jobName);

		boolean isAbstract = CoreNamespaceUtils.isAbstract(element);
		builder.setAbstract(isAbstract);

		String parentRef = element.getAttribute("parent");
		if (StringUtils.hasText(parentRef)) {
			builder.setParentName(parentRef);
		}

		String repositoryAttribute = element.getAttribute("job-repository");
		if (StringUtils.hasText(repositoryAttribute)) {
			builder.addPropertyReference("jobRepository", repositoryAttribute);
		}

		Element validator = DomUtils.getChildElementByTagName(element, "validator");
		if (validator != null) {
			builder.addPropertyValue("jobParametersValidator", parseBeanElement(validator, parserContext));
		}

		String restartableAttribute = element.getAttribute("restartable");
		if (StringUtils.hasText(restartableAttribute)) {
			builder.addPropertyValue("restartable", restartableAttribute);
		}

		String incrementer = (element.getAttribute("incrementer"));
		if (StringUtils.hasText(incrementer)) {
			builder.addPropertyReference("jobParametersIncrementer", incrementer);
		}

		if (isAbstract) {
			for (String tagName : Arrays.asList("step", "decision", "split")) {
				if (!DomUtils.getChildElementsByTagName(element, tagName).isEmpty()) {
					parserContext.getReaderContext().error(
							"The <" + tagName + "/> element may not appear on a <job/> with abstract=\"true\" ["
									+ jobName + "]", element);
				}
			}
		}
		else {
			InlineFlowParser flowParser = new InlineFlowParser(jobName, jobName);
			BeanDefinition flowDef = flowParser.parse(element, parserContext);
			builder.addPropertyValue("flow", flowDef);
		}

		Element description = DomUtils.getChildElementByTagName(element, "description");
		if (description != null) {
			builder.getBeanDefinition().setDescription(description.getTextContent());
		}

		List<Element> listenersElements = DomUtils.getChildElementsByTagName(element, "listeners");
		if (listenersElements.size() == 1) {
			Element listenersElement = listenersElements.get(0);
			CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(listenersElement.getTagName(),
					parserContext.extractSource(element));
			parserContext.pushContainingComponent(compositeDef);
			ManagedList listeners = new ManagedList();
			listeners.setMergeEnabled(listenersElement.hasAttribute(MERGE_ATTR)
					&& Boolean.valueOf(listenersElement.getAttribute(MERGE_ATTR)));
			List<Element> listenerElements = DomUtils.getChildElementsByTagName(listenersElement, "listener");
			for (Element listenerElement : listenerElements) {
				listeners.add(jobListenerParser.parse(listenerElement, parserContext));
			}
			builder.addPropertyValue("jobExecutionListeners", listeners);
			parserContext.popAndRegisterContainingComponent();
		}
		else if (listenersElements.size() > 1) {
			parserContext.getReaderContext().error(
					"The '<listeners/>' element may not appear more than once in a single <job/>.", element);
		}

	}

	public BeanMetadataElement parseBeanElement(Element element, ParserContext parserContext) {
		String refAttribute = element.getAttribute(REF_ATTR);
		Element beanElement = DomUtils.getChildElementByTagName(element, BEAN_ELE);
		Element refElement = DomUtils.getChildElementByTagName(element, REF_ELE);

		if (StringUtils.hasText(refAttribute)) {
			return new RuntimeBeanReference(refAttribute);
		}
		else if (beanElement != null) {
			BeanDefinitionHolder beanDefinitionHolder = parserContext.getDelegate().parseBeanDefinitionElement(
					beanElement);
			parserContext.getDelegate().decorateBeanDefinitionIfRequired(beanElement, beanDefinitionHolder);
			return beanDefinitionHolder;
		}
		else if (refElement != null) {
			return (BeanMetadataElement) parserContext.getDelegate().parsePropertySubElement(refElement, null);
		}

		parserContext.getReaderContext().error(
				"One of ref attribute or a nested bean definition or ref element must be specified", element);
		return null;
	}

}
