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

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parser for the lt;job-repository/gt; element in the Batch namespace. Sets up
 * and returns a JobRepositoryFactoryBean.
 * 
 * @author Thomas Risberg
 * @since 2.0
 * 
 */
public class JobRepositoryParser extends AbstractSingleBeanDefinitionParser {

	protected String getBeanClassName(Element element) {
		return "org.springframework.batch.core.repository.support.JobRepositoryFactoryBean";
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {

		String id = element.getAttribute(ID_ATTRIBUTE);
		if (!StringUtils.hasText(id)) {
			id = "jobRepository";
		}

		return id;

	}

	/**
	 * Parse and create a bean definition for a
	 * {@link org.springframework.batch.core.repository.support.JobRepositoryFactoryBean}
	 * .
	 */
	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

		CoreNamespaceUtils.autoregisterBeansForNamespace(parserContext, element);

		String dataSource = element.getAttribute("data-source");

		String transactionManager = element.getAttribute("transaction-manager");

		String isolationLevelForCreate = element.getAttribute("isolation-level-for-create");

		String tablePrefix = element.getAttribute("table-prefix");

		String maxVarCharLength = element.getAttribute("max-varchar-length");

		String lobHandler = element.getAttribute("lob-handler");

		RuntimeBeanReference ds = new RuntimeBeanReference(dataSource);
		builder.addPropertyValue("dataSource", ds);
		RuntimeBeanReference tx = new RuntimeBeanReference(transactionManager);
		builder.addPropertyValue("transactionManager", tx);
		if (StringUtils.hasText(isolationLevelForCreate)) {
			builder.addPropertyValue("isolationLevelForCreate", DefaultTransactionDefinition.PREFIX_ISOLATION
					+ isolationLevelForCreate);
		}
		if (StringUtils.hasText(tablePrefix)) {
			builder.addPropertyValue("tablePrefix", tablePrefix);
		}
		if (StringUtils.hasText(lobHandler)) {
			builder.addPropertyReference("lobHandler", lobHandler);
		}
		if (StringUtils.hasText(maxVarCharLength)) {
			builder.addPropertyValue("maxVarCharLength", maxVarCharLength);
		}

		builder.setRole(BeanDefinition.ROLE_SUPPORT);

	}
}
