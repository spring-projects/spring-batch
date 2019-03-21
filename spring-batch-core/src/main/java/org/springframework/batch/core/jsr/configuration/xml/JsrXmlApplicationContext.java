/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.xml;

import java.util.Properties;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;

/**
 * <p>
 * {@link GenericApplicationContext} implementation providing JSR-352 related context operations.
 * </p>
 *
 * @author Chris Schaefer
 * @since 3.0
 */
public class JsrXmlApplicationContext extends GenericApplicationContext {
	private static final String JOB_PARAMETERS_BEAN_DEFINITION_NAME = "jsr_jobParameters";

	private XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);

	/**
	 * <p>
	 * Create a new context instance with no job parameters.
	 * </p>
	 */
	public JsrXmlApplicationContext() {
		reader.setDocumentReaderClass(JsrBeanDefinitionDocumentReader.class);
		reader.setEnvironment(this.getEnvironment());
	}

	/**
	 * <p>
	 * Create a new context instance using the provided {@link Properties} representing job
	 * parameters when pre-processing the job definition document.
	 * </p>
	 *
	 * @param jobParameters the {@link Properties} representing job parameters
	 */
	public JsrXmlApplicationContext(Properties jobParameters) {
		reader.setDocumentReaderClass(JsrBeanDefinitionDocumentReader.class);
		reader.setEnvironment(this.getEnvironment());

		storeJobParameters(jobParameters);
	}

	private void storeJobParameters(Properties properties) {
		BeanDefinition jobParameters = BeanDefinitionBuilder.genericBeanDefinition(Properties.class).getBeanDefinition();
		jobParameters.getConstructorArgumentValues().addGenericArgumentValue(properties != null ? properties : new Properties());

		reader.getRegistry().registerBeanDefinition(JOB_PARAMETERS_BEAN_DEFINITION_NAME, jobParameters);
	}

	protected XmlBeanDefinitionReader getReader() {
		return reader;
	}

	/**
	 * Set whether to use XML validation. Default is <code>true</code>.
	 *
	 * @param validating true if XML should be validated.
	 */
	public void setValidating(boolean validating) {
		this.reader.setValidating(validating);
	}

	/**
	 * Load bean definitions from the given XML resources.
	 * @param resources one or more resources to load from
	 */
	public void load(Resource... resources) {
		this.reader.loadBeanDefinitions(resources);
	}
}
