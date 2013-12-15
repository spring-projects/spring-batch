package org.springframework.batch.core.jsr.configuration.xml;

import java.util.Properties;
import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * <p>
 * Test cases around {@link JsrXmlApplicationContext}.
 * </p>
 *
 * @author Chris Schaefer
 */
public class JsrXmlApplicationContextTests {
	private static final String JOB_PARAMETERS_BEAN_DEFINITION_NAME = "jsr_jobParameters";

	@Test
	public void testNullProperties() {
		JsrXmlApplicationContext applicationContext = new JsrXmlApplicationContext(null);

		BeanDefinition beanDefinition = applicationContext.getBeanDefinition(JOB_PARAMETERS_BEAN_DEFINITION_NAME);
		Properties properties = (Properties) beanDefinition.getConstructorArgumentValues().getGenericArgumentValue(Properties.class).getValue();

		assertNotNull("Properties should not be null", properties);
		assertTrue("Properties should be empty", properties.isEmpty());
	}

	@Test
	public void testWithProperties() {
		Properties properties = new Properties();
		properties.put("prop1key", "prop1val");

		JsrXmlApplicationContext applicationContext = new JsrXmlApplicationContext(properties);

		BeanDefinition beanDefinition = applicationContext.getBeanDefinition(JOB_PARAMETERS_BEAN_DEFINITION_NAME);
		Properties storedProperties = (Properties) beanDefinition.getConstructorArgumentValues().getGenericArgumentValue(Properties.class).getValue();

		assertNotNull("Properties should not be null", storedProperties);
		assertFalse("Properties not be empty", storedProperties.isEmpty());
		assertEquals("prop1val", storedProperties.getProperty("prop1key"));
	}
}
