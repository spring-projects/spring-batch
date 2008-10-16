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
package org.springframework.batch.core.configuration.xml;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.job.ConditionalJob;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parser for the lt;job/gt; element in the Batch namespace. Sets up and returns
 * a bean definition for a {@link Job}.
 * 
 * @author Dave Syer
 * 
 */
public class JobParser extends AbstractBeanDefinitionParser {

	/**
	 * Create a bean definition for a {@link ConditionalJob}. The
	 * <code>jobRepository</code> attribute is a reference to a
	 * {@link JobRepository} and defaults to "jobRepository". Nested step
	 * elements are delegated to a {@link StepParser}.
	 * 
	 * @see AbstractBeanDefinitionParser#parseInternal(Element, ParserContext)
	 */
	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ConditionalJob.class);
		String repositoryAttribute = element.getAttribute("repository");
		if (!StringUtils.hasText(repositoryAttribute)) {
			repositoryAttribute = "jobRepository";
		}
		builder.addPropertyReference("jobRepository", repositoryAttribute);

		StepParser stepParser = new StepParser();
		List<RuntimeBeanReference> stepTransitions = new ArrayList<RuntimeBeanReference>();

		@SuppressWarnings("unchecked")
		List<Element> stepElements = (List<Element>) DomUtils.getChildElementsByTagName(element, "step");
		for (Element stepElement : stepElements) {
			stepTransitions.addAll(stepParser.parse(stepElement, parserContext));
		}
		ManagedList managedList = new ManagedList();
		@SuppressWarnings( { "unchecked", "unused" })
		boolean dummy = managedList.addAll(stepTransitions);
		builder.addPropertyValue("stepTransitions", managedList);

		return builder.getBeanDefinition();
	}

}
