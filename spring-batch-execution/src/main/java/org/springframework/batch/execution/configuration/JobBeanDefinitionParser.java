package org.springframework.batch.execution.configuration;

import org.springframework.batch.execution.step.tasklet.TaskletStep;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanNameReference;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.parsing.ParseState;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class JobBeanDefinitionParser implements BeanDefinitionParser {

	private static final String TAG_JOB = "job";

	private static final String TAG_CHUNKING_STEP = "chunking-step";

	private static final String TAG_TASKLET_STEP = "tasklet-step";

	private static final String ATT_ID = "id";

	private static final String ATT_TASKLET = "tasklet";

	private static final String ATT_RERUN = "rerun";

	private static final String PROP_TASKLET = "tasklet";

	private final ParseState parseState = new ParseState();

	public BeanDefinition parse(Element element, ParserContext parserContext) {
		CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(),
		        parserContext.extractSource(element));
		parserContext.pushContainingComponent(compositeDef);

		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				String localName = node.getLocalName();
				if (TAG_CHUNKING_STEP.equals(localName)) {
//					parseChunkingStep((Element) node, parserContext);
				} else if (TAG_TASKLET_STEP.equals(localName)) {
					parseTaskletStep((Element) node, parserContext);
				}
			}
		}

		parserContext.popAndRegisterContainingComponent();
		return null;
	}

	private void parseTaskletStep(Element taskletElement, ParserContext parserContext) {
		AbstractBeanDefinition taskletStepDef = createTaskletStepBeanDefinition(taskletElement, parserContext);
	}

	private AbstractBeanDefinition createTaskletStepBeanDefinition(Element taskletElement, ParserContext parserContext) {
		RootBeanDefinition taskletStepDefinition = new RootBeanDefinition(TaskletStep.class);
		taskletStepDefinition.setSource(parserContext.extractSource(taskletElement));

		String tasklet = taskletElement.getAttribute(ATT_TASKLET);
		if (!StringUtils.hasText(tasklet)) {
			parserContext.getReaderContext().error("'tasklet' attribute contains empty value", taskletElement,
			        parseState.snapshot());
		} else {
			taskletStepDefinition.getPropertyValues().addPropertyValue(PROP_TASKLET,
			        new RuntimeBeanNameReference(tasklet));
		}
		
		System.out.println(taskletElement.hasAttribute("rerun"));
		System.out.println(taskletElement.getAttribute("rerun"));
		
		return taskletStepDefinition;
	}
}
