/*
 * Copyright 2013-2017 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.batch.api.Batchlet;
import javax.batch.runtime.JobExecution;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

import org.springframework.batch.core.jsr.AbstractJsrTestCase;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.DefaultDocumentLoader;
import org.springframework.beans.factory.xml.DelegatingEntityResolver;
import org.springframework.beans.factory.xml.DocumentLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.SimpleSaxErrorHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * <p>
 * Test cases around {@link JsrBeanDefinitionDocumentReader}.
 * </p>
 *
 * @author Chris Schaefer
 */
public class JsrBeanDefinitionDocumentReaderTests extends AbstractJsrTestCase {
	private static final String JOB_PARAMETERS_BEAN_DEFINITION_NAME = "jsr_jobParameters";

	private Log logger = LogFactory.getLog(getClass());
	private DocumentLoader documentLoader = new DefaultDocumentLoader();
	private ErrorHandler errorHandler = new SimpleSaxErrorHandler(logger);

	@Test
	@SuppressWarnings("resource")
	public void testGetJobParameters() {
		Properties jobParameters = new Properties();
		jobParameters.setProperty("jobParameter1", "jobParameter1Value");
		jobParameters.setProperty("jobParameter2", "jobParameter2Value");

		JsrXmlApplicationContext applicationContext = new JsrXmlApplicationContext(jobParameters);
		applicationContext.setValidating(false);
		applicationContext.load(new ClassPathResource("jsrBaseContext.xml"),
				new ClassPathResource("/META-INF/batch.xml"),
				new ClassPathResource("/META-INF/batch-jobs/jsrPropertyPreparseTestJob.xml"));
		applicationContext.refresh();

		BeanDefinition beanDefinition = applicationContext.getBeanDefinition(JOB_PARAMETERS_BEAN_DEFINITION_NAME);

		Properties processedJobParameters = (Properties) beanDefinition.getConstructorArgumentValues().getGenericArgumentValue(Properties.class).getValue();
		assertNotNull(processedJobParameters);
		assertTrue("Wrong number of job parameters", processedJobParameters.size() == 2);
		assertEquals("jobParameter1Value", processedJobParameters.getProperty("jobParameter1"));
		assertEquals("jobParameter2Value", processedJobParameters.getProperty("jobParameter2"));
	}

	@Test
	public void testGetJobProperties() {
		Document document = getDocument("/META-INF/batch-jobs/jsrPropertyPreparseTestJob.xml");

		@SuppressWarnings("resource")
		JsrXmlApplicationContext applicationContext = new JsrXmlApplicationContext();
		JsrBeanDefinitionDocumentReader documentReader = new JsrBeanDefinitionDocumentReader(applicationContext);
		documentReader.initProperties(document.getDocumentElement());

		Properties documentJobProperties = documentReader.getJobProperties();
		assertNotNull(documentJobProperties);
		assertTrue("Wrong number of job properties", documentJobProperties.size() == 3);
		assertEquals("jobProperty1Value", documentJobProperties.getProperty("jobProperty1"));
		assertEquals("jobProperty1Value", documentJobProperties.getProperty("jobProperty2"));
		assertEquals("", documentJobProperties.getProperty("jobProperty3"));
	}

	@Test
	public void testJobParametersResolution() {
		Properties jobParameters = new Properties();
		jobParameters.setProperty("jobParameter1", "myfile.txt");
		jobParameters.setProperty("jobParameter2", "#{jobProperties['jobProperty2']}");
		jobParameters.setProperty("jobParameter3", "#{jobParameters['jobParameter1']}");

		@SuppressWarnings("resource")
		JsrXmlApplicationContext applicationContext = new JsrXmlApplicationContext(jobParameters);
		applicationContext.setValidating(false);
		applicationContext.load(new ClassPathResource("jsrBaseContext.xml"),
				new ClassPathResource("/META-INF/batch.xml"),
				new ClassPathResource("/META-INF/batch-jobs/jsrPropertyPreparseTestJob.xml"));
		applicationContext.refresh();

		Document document = getDocument("/META-INF/batch-jobs/jsrPropertyPreparseTestJob.xml");

		JsrBeanDefinitionDocumentReader documentReader = new JsrBeanDefinitionDocumentReader(applicationContext);
		documentReader.initProperties(document.getDocumentElement());

		Properties resolvedParameters = documentReader.getJobParameters();

		assertNotNull(resolvedParameters);
		assertTrue("Wrong number of job parameters", resolvedParameters.size() == 3);
		assertEquals("myfile.txt", resolvedParameters.getProperty("jobParameter1"));
		assertEquals("jobProperty1Value", resolvedParameters.getProperty("jobParameter2"));
		assertEquals("myfile.txt", resolvedParameters.getProperty("jobParameter3"));
	}

	@Test
	public void testJobPropertyResolution() {
		Properties jobParameters = new Properties();
		jobParameters.setProperty("file.name", "myfile.txt");

		@SuppressWarnings("resource")
		JsrXmlApplicationContext applicationContext = new JsrXmlApplicationContext(jobParameters);
		applicationContext.setValidating(false);
		applicationContext.load(new ClassPathResource("jsrBaseContext.xml"),
				new ClassPathResource("/META-INF/batch.xml"),
				new ClassPathResource("/META-INF/batch-jobs/jsrPropertyPreparseTestJob.xml"));
		applicationContext.refresh();

		Document document = getDocument("/META-INF/batch-jobs/jsrPropertyPreparseTestJob.xml");

		JsrBeanDefinitionDocumentReader documentReader = new JsrBeanDefinitionDocumentReader(applicationContext);
		documentReader.initProperties(document.getDocumentElement());

		Properties resolvedProperties = documentReader.getJobProperties();
		assertNotNull(resolvedProperties);
		assertTrue("Wrong number of job properties", resolvedProperties.size() == 3);
		assertEquals("jobProperty1Value", resolvedProperties.getProperty("jobProperty1"));
		assertEquals("jobProperty1Value", resolvedProperties.getProperty("jobProperty2"));
		assertEquals("myfile.txt", resolvedProperties.getProperty("jobProperty3"));
	}

	@SuppressWarnings("resource")
	@Test
	public void testGenerationOfBeanDefinitionsForMultipleReferences() throws Exception {
		JsrXmlApplicationContext applicationContext = new JsrXmlApplicationContext(new Properties());
		applicationContext.setValidating(false);
		applicationContext.load(new ClassPathResource("jsrBaseContext.xml"),
				new ClassPathResource("/META-INF/batch.xml"),
				new ClassPathResource("/META-INF/batch-jobs/jsrUniqueInstanceTests.xml"));
		applicationContext.refresh();

		assertTrue("exitStatusSettingStepListener bean definition not found", applicationContext.containsBeanDefinition("exitStatusSettingStepListener"));
		assertTrue("exitStatusSettingStepListener1 bean definition not found", applicationContext.containsBeanDefinition("exitStatusSettingStepListener1"));
		assertTrue("exitStatusSettingStepListener2 bean definition not found", applicationContext.containsBeanDefinition("exitStatusSettingStepListener2"));
		assertTrue("exitStatusSettingStepListener3 bean definition not found", applicationContext.containsBeanDefinition("exitStatusSettingStepListener3"));
		assertTrue("exitStatusSettingStepListenerClassBeanDefinition bean definition not found", applicationContext.containsBeanDefinition("org.springframework.batch.core.jsr.step.listener.ExitStatusSettingStepListener"));
		assertTrue("exitStatusSettingStepListener1ClassBeanDefinition bean definition not found", applicationContext.containsBeanDefinition("org.springframework.batch.core.jsr.step.listener.ExitStatusSettingStepListener1"));
		assertTrue("exitStatusSettingStepListener2ClassBeanDefinition bean definition not found", applicationContext.containsBeanDefinition("org.springframework.batch.core.jsr.step.listener.ExitStatusSettingStepListener2"));
		assertTrue("exitStatusSettingStepListener3ClassBeanDefinition bean definition not found", applicationContext.containsBeanDefinition("org.springframework.batch.core.jsr.step.listener.ExitStatusSettingStepListener3"));
		assertTrue("testBatchlet bean definition not found", applicationContext.containsBeanDefinition("testBatchlet"));
		assertTrue("testBatchlet1 bean definition not found", applicationContext.containsBeanDefinition("testBatchlet1"));
	}

	@Test
	public void testArtifactUniqueness() throws Exception {
		JobExecution jobExecution = runJob("jsrUniqueInstanceTests", new Properties(), 10000L);
		String exitStatus = jobExecution.getExitStatus();

		assertTrue("Exit status must contain listener3", exitStatus.contains("listener3"));
		exitStatus = exitStatus.replace("listener3", "");

		assertTrue("Exit status must contain listener2", exitStatus.contains("listener2"));
		exitStatus = exitStatus.replace("listener2", "");

		assertTrue("Exit status must contain listener1", exitStatus.contains("listener1"));
		exitStatus = exitStatus.replace("listener1", "");

		assertTrue("Exit status must contain listener0", exitStatus.contains("listener0"));
		exitStatus = exitStatus.replace("listener0", "");

		assertTrue("Exit status must contain listener7", exitStatus.contains("listener7"));
		exitStatus = exitStatus.replace("listener7", "");

		assertTrue("Exit status must contain listener6", exitStatus.contains("listener6"));
		exitStatus = exitStatus.replace("listener6", "");

		assertTrue("Exit status must contain listener5", exitStatus.contains("listener5"));
		exitStatus = exitStatus.replace("listener5", "");

		assertTrue("Exit status must contain listener4", exitStatus.contains("listener4"));
		exitStatus = exitStatus.replace("listener4", "");

		assertTrue("exitStatus must be empty", "".equals(exitStatus));
	}

	@Test
	@SuppressWarnings("resource")
	public void testGenerationOfSpringBeanDefinitionsForMultipleReferences() {
		JsrXmlApplicationContext applicationContext = new JsrXmlApplicationContext(new Properties());
		applicationContext.setValidating(false);
		applicationContext.load(new ClassPathResource("jsrBaseContext.xml"),
				new ClassPathResource("/META-INF/batch-jobs/jsrSpringInstanceTests.xml"));

		applicationContext.refresh();

		assertTrue("exitStatusSettingStepListener bean definition not found", applicationContext.containsBeanDefinition("exitStatusSettingStepListener"));
		assertTrue("scopedTarget.exitStatusSettingStepListener bean definition not found", applicationContext.containsBeanDefinition("scopedTarget.exitStatusSettingStepListener"));

		BeanDefinition exitStatusSettingStepListenerBeanDefinition = applicationContext.getBeanDefinition("scopedTarget.exitStatusSettingStepListener");
		assertTrue("step".equals(exitStatusSettingStepListenerBeanDefinition.getScope()));

		assertTrue("Should not contain bean definition for exitStatusSettingStepListener1", !applicationContext.containsBeanDefinition("exitStatusSettingStepListener1"));
		assertTrue("Should not contain bean definition for exitStatusSettingStepListener2", !applicationContext.containsBeanDefinition("exitStatusSettingStepListener2"));
		assertTrue("Should not contain bean definition for exitStatusSettingStepListener3", !applicationContext.containsBeanDefinition("exitStatusSettingStepListener3"));

		assertTrue("Should not contain bean definition for testBatchlet1", !applicationContext.containsBeanDefinition("testBatchlet1"));
		assertTrue("Should not contain bean definition for testBatchlet2", !applicationContext.containsBeanDefinition("testBatchlet2"));

		assertTrue("testBatchlet bean definition not found", applicationContext.containsBeanDefinition("testBatchlet"));

		BeanDefinition testBatchletBeanDefinition = applicationContext.getBeanDefinition("testBatchlet");
		assertTrue("singleton".equals(testBatchletBeanDefinition.getScope()));
	}

	@Test
	public void testSpringArtifactUniqueness() throws Exception {
		JobExecution jobExecution = runJob("jsrSpringInstanceTests", new Properties(), 10000L);
		String exitStatus = jobExecution.getExitStatus();

		assertTrue("Exit status must contain listener1", exitStatus.contains("listener1"));
		assertTrue("exitStatus must contain 2 listener1 values", StringUtils.countOccurrencesOf(exitStatus, "listener1") == 2);

		exitStatus = exitStatus.replace("listener1", "");

		assertTrue("Exit status must contain listener4", exitStatus.contains("listener4"));
		assertTrue("exitStatus must contain 2 listener4 values", StringUtils.countOccurrencesOf(exitStatus, "listener4") == 2);
		exitStatus = exitStatus.replace("listener4", "");

		assertTrue("exitStatus must be empty", "".equals(exitStatus));
	}

	private Document getDocument(String location) {
		InputStream inputStream = this.getClass().getResourceAsStream(location);

		try {
			return documentLoader.loadDocument(new InputSource(inputStream),
				new DelegatingEntityResolver(getClass().getClassLoader()), errorHandler, 0, true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) { }
		}
	}

	public static class TestBatchlet implements Batchlet {
		@Override
		public String process() throws Exception {
			return null;
		}

		@Override
		public void stop() throws Exception {

		}
	}
}
