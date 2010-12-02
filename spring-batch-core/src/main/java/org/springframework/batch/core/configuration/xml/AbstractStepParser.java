/*
 * Copyright 2006-2009 the original author or authors.
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

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Internal parser for the &lt;step/&gt; elements inside a job. A step element
 * references a bean definition for a
 * {@link org.springframework.batch.core.Step} and goes on to (optionally) list
 * a set of transitions from that step to others with &lt;next on="pattern"
 * to="stepName"/&gt;. Used by the {@link JobParser}.
 *
 * @author Dave Syer
 * @author Thomas Risberg
 * @see JobParser
 * @since 2.0
 */
public abstract class AbstractStepParser {

	protected static final String ID_ATTR = "id";

	private static final String PARENT_ATTR = "parent";

	private static final String REF_ATTR = "ref";

	private static final String TASKLET_ELE = "tasklet";

	private static final String PARTITION_ELE = "partition";

	private static final String JOB_ELE = "job";

	private static final String JOB_PARAMS_EXTRACTOR_ATTR = "job-parameters-extractor";

	private static final String JOB_LAUNCHER_ATTR = "job-launcher";

	private static final String STEP_ATTR = "step";

	private static final String STEP_ELE = STEP_ATTR;

	private static final String PARTITIONER_ATTR = "partitioner";

	private static final String HANDLER_ATTR = "handler";

	private static final String HANDLER_ELE = "handler";

	private static final String TASK_EXECUTOR_ATTR = "task-executor";

	private static final String GRID_SIZE_ATTR = "grid-size";

	private static final String FLOW_ELE = "flow";

	private static final String JOB_REPO_ATTR = "job-repository";

	/**
	 * @param stepElement   The &lt;step/&gt; element
	 * @param parserContext
	 * @param jobFactoryRef the reference to the {@link JobParserJobFactoryBean}
	 *                      from the enclosing tag. Use 'null' if unknown.
	 */
	protected AbstractBeanDefinition parseStep(Element stepElement, ParserContext parserContext, String jobFactoryRef) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
		AbstractBeanDefinition bd = builder.getRawBeanDefinition();

		Element taskletElement = DomUtils.getChildElementByTagName(stepElement, TASKLET_ELE);
		if (taskletElement != null) {
			boolean stepUnderspecified = CoreNamespaceUtils.isUnderspecified(stepElement);
			new TaskletParser().parseTasklet(stepElement, taskletElement, bd, parserContext, stepUnderspecified);
		}

		Element flowElement = DomUtils.getChildElementByTagName(stepElement, FLOW_ELE);
		if (flowElement != null) {
			boolean stepUnderspecified = CoreNamespaceUtils.isUnderspecified(stepElement);
			parseFlow(stepElement, flowElement, bd, parserContext, stepUnderspecified);
		}

		Element partitionElement = DomUtils.getChildElementByTagName(stepElement, PARTITION_ELE);
		if (partitionElement != null) {
			boolean stepUnderspecified = CoreNamespaceUtils.isUnderspecified(stepElement);
			parsePartition(stepElement, partitionElement, bd, parserContext, stepUnderspecified, jobFactoryRef);
		}

		Element jobElement = DomUtils.getChildElementByTagName(stepElement, JOB_ELE);
		if (jobElement != null) {
			boolean stepUnderspecified = CoreNamespaceUtils.isUnderspecified(stepElement);
			parseJob(stepElement, jobElement, bd, parserContext, stepUnderspecified);
		}

		String parentRef = stepElement.getAttribute(PARENT_ATTR);
		if (StringUtils.hasText(parentRef)) {
			bd.setParentName(parentRef);
		}

		String isAbstract = stepElement.getAttribute("abstract");
		if (StringUtils.hasText(isAbstract)) {
			bd.setAbstract(Boolean.valueOf(isAbstract));
		}

		String jobRepositoryRef = stepElement.getAttribute(JOB_REPO_ATTR);
		if (StringUtils.hasText(jobRepositoryRef)) {
			builder.addPropertyReference("jobRepository", jobRepositoryRef);
		}

		if (StringUtils.hasText(jobFactoryRef)) {
			bd.setAttribute("jobParserJobFactoryBeanRef", jobFactoryRef);
		}

		Element description = DomUtils.getChildElementByTagName(stepElement, "description");
		if (description != null) {
			bd.setDescription(description.getTextContent());
		}

		return bd;

	}

	private void parsePartition(Element stepElement, Element partitionElement, AbstractBeanDefinition bd, ParserContext parserContext, boolean stepUnderspecified, String jobFactoryRef ) {

		bd.setBeanClass(StepParserStepFactoryBean.class);
		bd.setAttribute("isNamespaceStep", true);
		String stepRef = partitionElement.getAttribute(STEP_ATTR);
		String partitionerRef = partitionElement.getAttribute(PARTITIONER_ATTR);
		String handlerRef = partitionElement.getAttribute(HANDLER_ATTR);

		if (!StringUtils.hasText(partitionerRef)) {
			parserContext.getReaderContext().error("You must specify a partitioner", partitionElement);
			return;
		}

		Element inlineStepElement = DomUtils.getChildElementByTagName(partitionElement, STEP_ELE);
		if (inlineStepElement == null && !StringUtils.hasText(stepRef)) {
			parserContext.getReaderContext().error("You must specify a step", partitionElement);
			return;
		}


		MutablePropertyValues propertyValues = bd.getPropertyValues();

		if (StringUtils.hasText(stepRef)) {
			propertyValues.addPropertyValue("step", new RuntimeBeanReference(stepRef));
		} else if( inlineStepElement!=null) {
			AbstractBeanDefinition stepDefinition = parseStep(inlineStepElement, parserContext, jobFactoryRef);
			propertyValues.addPropertyValue("step", stepDefinition );
		}

		propertyValues.addPropertyValue("partitioner", new RuntimeBeanReference(partitionerRef));

		if (!StringUtils.hasText(handlerRef)) {
			Element handlerElement = DomUtils.getChildElementByTagName(partitionElement, HANDLER_ELE);
			if (handlerElement != null) {
				String taskExecutorRef = handlerElement.getAttribute(TASK_EXECUTOR_ATTR);
				if (StringUtils.hasText(taskExecutorRef)) {
					propertyValues.addPropertyValue("taskExecutor", new RuntimeBeanReference(taskExecutorRef));
				}
				String gridSize = handlerElement.getAttribute(GRID_SIZE_ATTR);
				if (StringUtils.hasText(gridSize)) {
					propertyValues.addPropertyValue("gridSize", new TypedStringValue(gridSize));
				}
			}
		} else {
			propertyValues.addPropertyValue("partitionHandler", new RuntimeBeanReference(handlerRef));
		}

	}

	private void parseJob(Element stepElement, Element jobElement, AbstractBeanDefinition bd, ParserContext parserContext, boolean stepUnderspecified) {

		bd.setBeanClass(StepParserStepFactoryBean.class);
		bd.setAttribute("isNamespaceStep", true);
		String jobRef = jobElement.getAttribute(REF_ATTR);

		if (!StringUtils.hasText(jobRef)) {
			parserContext.getReaderContext().error("You must specify a job", jobElement);
			return;
		}

		MutablePropertyValues propertyValues = bd.getPropertyValues();
		propertyValues.addPropertyValue("job", new RuntimeBeanReference(jobRef));

		String jobParametersExtractor = jobElement.getAttribute(JOB_PARAMS_EXTRACTOR_ATTR);
		String jobLauncher = jobElement.getAttribute(JOB_LAUNCHER_ATTR);

		if (StringUtils.hasText(jobParametersExtractor)) {
			propertyValues.addPropertyValue("jobParametersExtractor", new RuntimeBeanReference(jobParametersExtractor));
		}
		if (StringUtils.hasText(jobLauncher)) {
			propertyValues.addPropertyValue("jobLauncher", new RuntimeBeanReference(jobLauncher));
		}

	}


	private void parseFlow(Element stepElement, Element flowElement, AbstractBeanDefinition bd,
	                       ParserContext parserContext, boolean stepUnderspecified) {

		bd.setBeanClass(StepParserStepFactoryBean.class);
		bd.setAttribute("isNamespaceStep", true);
		String flowRef = flowElement.getAttribute(PARENT_ATTR);
		String idAttribute = stepElement.getAttribute(ID_ATTR);

		BeanDefinition flowDefinition = new GenericBeanDefinition();
		flowDefinition.setParentName(flowRef);
		MutablePropertyValues propertyValues = flowDefinition.getPropertyValues();
		if (StringUtils.hasText(idAttribute)) {
			propertyValues.addPropertyValue("name", idAttribute);
		}

		bd.getPropertyValues().addPropertyValue("flow", flowDefinition);

	}

}
