/*
 * Copyright 2006-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.configuration.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceArrayPropertyEditor;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

class StepNameTests {

	@Nullable
	private ApplicationContext getContextFromResource(Resource resource) throws IOException {
		try {
			return new FileSystemXmlApplicationContext("file:///" + resource.getFile().getAbsolutePath());
		}
		catch (BeanDefinitionParsingException | BeanCreationException e) {
			return null;
		}
	}

	@MethodSource
	@ParameterizedTest
	void testStepNames(Resource resource) throws Exception {
		ApplicationContext context = getContextFromResource(resource);
		if (context == null) {
			return;
		}
		Map<String, StepLocator> stepLocators = context.getBeansOfType(StepLocator.class);
		for (String name : stepLocators.keySet()) {
			StepLocator stepLocator = stepLocators.get(name);
			Collection<String> stepNames = stepLocator.getStepNames();
			Job job = context.getBean(name, Job.class);
			String jobName = job.getName();
			assertFalse(stepNames.isEmpty(), "Job has no steps: " + jobName);
			for (String registeredName : stepNames) {
				String stepName = stepLocator.getStep(registeredName).getName();
				assertEquals(stepName, registeredName, "Step name not equal to registered value: " + stepName + "!="
						+ registeredName + ", " + jobName);
			}
		}
	}

	static List<Arguments> testStepNames() throws Exception {
		List<Arguments> list = new ArrayList<>();
		ResourceArrayPropertyEditor editor = new ResourceArrayPropertyEditor();
		editor.setAsText("classpath*:" + ClassUtils.addResourcePathToPackagePath(StepNameTests.class, "*.xml"));
		Resource[] resources = (Resource[]) editor.getValue();
		for (Resource resource : resources) {
			if (resource.getFile().getName().contains("WrongSchema")) {
				continue;
			}
			list.add(Arguments.of(resource));
		}
		return list;
	}

}
