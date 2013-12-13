package org.springframework.batch.core.jsr.configuration.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.DefaultDocumentLoader;
import org.springframework.beans.factory.xml.DelegatingEntityResolver;
import org.springframework.beans.factory.xml.DocumentLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.xml.SimpleSaxErrorHandler;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * <p>
 * Test cases around {@link JsrBeanDefinitionDocumentReader}.
 * </p>
 *
 * @author Chris Schaefer
 */
public class JsrBeanDefinitionDocumentReaderTests {
	private static final String JOB_PARAMETERS_BEAN_DEFINITION_NAME = "jsr_jobParameters";

	private Log logger = LogFactory.getLog(getClass());
	private DocumentLoader documentLoader = new DefaultDocumentLoader();
	private ErrorHandler errorHandler = new SimpleSaxErrorHandler(logger);

	@Test
	public void testGetJobParameters() {
		Properties jobParameters = new Properties();
		jobParameters.setProperty("jobParameter1", "jobParameter1Value");
		jobParameters.setProperty("jobParameter2", "jobParameter2Value");

		JsrXmlApplicationContext applicationContext = new JsrXmlApplicationContext(jobParameters);
		applicationContext.setValidating(false);
		applicationContext.load(new ClassPathResource("baseContext.xml"),
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

		JsrXmlApplicationContext applicationContext = new JsrXmlApplicationContext(jobParameters);
		applicationContext.setValidating(false);
		applicationContext.load(new ClassPathResource("baseContext.xml"),
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

		JsrXmlApplicationContext applicationContext = new JsrXmlApplicationContext(jobParameters);
		applicationContext.setValidating(false);
		applicationContext.load(new ClassPathResource("baseContext.xml"),
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

	private Document getDocument(String location) {
		InputStream inputStream = ClassLoader.class.getResourceAsStream(location);

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
}
