/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.batch.execution.configuration;

import org.springframework.batch.execution.step.TaskletStep;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Ben Hale
 */
class JobBeanDefinitionParser implements BeanDefinitionParser {

	private static final String TAG_JOB = "job";

	private static final String TAG_CHUNKING_STEP = "chunking-step";

	private static final String TAG_TASKLET_STEP = "tasklet-step";

	private static final String ATT_ID = "id";

	private static final String ATT_TASKLET = "tasklet";

	private static final String ATT_RERUN = "rerun";

	private static final String PROP_TASKLET = "tasklet";

	private static final String ENUM_ALWAYS = "always";

	private static final String ENUM_NEVER = "never";

	private static final String ENUM_INCOMPLETE = "incomplete";

	private static final String PROP_ALLOW_START_IF_COMPLETE = "allowStartIfComplete";

	private static final String PROP_JOB_REPOSITORY = "jobRepository";

	private static final String PROP_START_LIMIT = "startLimit";

	private static final String REPOSITORY_BEAN_NAME = "_jobRepository";

	public BeanDefinition parse(Element element, ParserContext parserContext) {
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				String localName = node.getLocalName();
				if (TAG_CHUNKING_STEP.equals(localName)) {
					// parseChunkingStep((Element) node, parserContext);
				} else if (TAG_TASKLET_STEP.equals(localName)) {
					parseTaskletStep((Element) node, parserContext);
				}
			}
		}
		return null;
	}

	private void parseTaskletStep(Element taskletElement, ParserContext parserContext) {
		AbstractBeanDefinition stepDef = createTaskletStepBeanDefinition(taskletElement, parserContext);
		String id = taskletElement.getAttribute(ATT_ID);

		if (StringUtils.hasText(id)) {
			parserContext.getRegistry().registerBeanDefinition(id, stepDef);
		} else {
			parserContext.getReaderContext().registerWithGeneratedName(stepDef);
		}
	}

	private AbstractBeanDefinition createTaskletStepBeanDefinition(Element taskletElement, ParserContext parserContext) {
		RootBeanDefinition stepDef = new RootBeanDefinition(TaskletStep.class);
		stepDef.setSource(parserContext.extractSource(taskletElement));

		String tasklet = taskletElement.getAttribute(ATT_TASKLET);
		if (!StringUtils.hasText(tasklet)) {
			parserContext.getReaderContext().error("'tasklet' attribute contains empty value", taskletElement);
		} else {
			stepDef.getPropertyValues().addPropertyValue(PROP_TASKLET, new RuntimeBeanReference(tasklet));
		}

		String rerun = taskletElement.getAttribute(ATT_RERUN);
		setPropertiesForRerun(rerun, stepDef.getPropertyValues());
		stepDef.getPropertyValues().addPropertyValue(PROP_JOB_REPOSITORY,
		        new RuntimeBeanReference(REPOSITORY_BEAN_NAME));

		return stepDef;
	}

	private void setPropertiesForRerun(String rerun, MutablePropertyValues propertyValues) {
		if (ENUM_ALWAYS.equals(rerun)) {
			propertyValues.addPropertyValue(PROP_ALLOW_START_IF_COMPLETE, Boolean.TRUE);
			propertyValues.addPropertyValue(PROP_START_LIMIT, new Integer(Integer.MAX_VALUE));
		} else if (ENUM_NEVER.equals(rerun)) {
			propertyValues.addPropertyValue(PROP_ALLOW_START_IF_COMPLETE, Boolean.FALSE);
			propertyValues.addPropertyValue(PROP_START_LIMIT, Integer.valueOf(1));
		} else if (ENUM_INCOMPLETE.equals(rerun)) {
			propertyValues.addPropertyValue(PROP_ALLOW_START_IF_COMPLETE, Boolean.FALSE);
			propertyValues.addPropertyValue(PROP_START_LIMIT, new Integer(Integer.MAX_VALUE));
		}
	}
}
