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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceArrayPropertyEditor;
import org.springframework.util.ClassUtils;

@RunWith(Parameterized.class)
public class StepNameTests {

	private Map<String, StepLocator> stepLocators = new HashMap<String, StepLocator>();

	private ApplicationContext context;

	public StepNameTests(Resource resource) throws Exception {
		try {
			context = new FileSystemXmlApplicationContext("file:///" + resource.getFile().getAbsolutePath());
		}
		catch (BeanDefinitionParsingException e) {
			return;
		}
		catch (BeanCreationException e) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, StepLocator> stepLocators = context.getBeansOfType(StepLocator.class);
		this.stepLocators = stepLocators;
	}

	@Test
	public void testStepNames() throws Exception {
		for (String name : stepLocators.keySet()) {
			StepLocator stepLocator = stepLocators.get(name);
			Collection<String> stepNames = stepLocator.getStepNames();
			Job job = (Job) context.getBean(name);
			String jobName = job.getName();
			assertTrue("Job has no steps: "+jobName, !stepNames.isEmpty());
			for (String registeredName : stepNames) {
				String stepName = stepLocator.getStep(registeredName).getName();
				assertEquals("Step name not equal to registered value: " + stepName + "!=" + registeredName + ", " + jobName,
						stepName, registeredName);
			}
		}
	}

	@Parameters
	public static List<Object[]> data() throws Exception {
		List<Object[]> list = new ArrayList<Object[]>();
		ResourceArrayPropertyEditor editor = new ResourceArrayPropertyEditor();
		editor.setAsText("classpath*:" + ClassUtils.addResourcePathToPackagePath(StepNameTests.class, "*.xml"));
		Resource[] resources = (Resource[]) editor.getValue();
		for (Resource resource : resources) {
			if (resource.getFile().getName().contains("WrongSchema")) {
				continue;
			}
			list.add(new Object[] { resource });
		}
		return list;
	}

}
