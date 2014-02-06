package org.springframework.batch.core.jsr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.PooledEmbeddedDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

public class JsrJobParametersConverterTests {

	private JsrJobParametersConverter converter;
	private static DataSource dataSource;

	@BeforeClass
	public static void setupDatabase() {
		dataSource = new PooledEmbeddedDataSource(new EmbeddedDatabaseBuilder().
				addScript("classpath:org/springframework/batch/core/schema-drop-hsqldb.sql").
				addScript("classpath:org/springframework/batch/core/schema-hsqldb.sql").
				build());
	}

	@Before
	public void setUp() throws Exception {
		converter = new JsrJobParametersConverter(dataSource);
		converter.afterPropertiesSet();
	}

	@Test
	public void testNullJobParameters() {
		Properties props = converter.getProperties((JobParameters) null);
		assertNotNull(props);
		Set<Entry<Object, Object>> properties = props.entrySet();
		assertEquals(1, properties.size());
		assertTrue(props.containsKey(JsrJobParametersConverter.JOB_RUN_ID));
	}

	@Test
	public void testStringJobParameters() {
		JobParameters parameters = new JobParametersBuilder().addString("key", "value", false).toJobParameters();
		Properties props = converter.getProperties(parameters);
		assertNotNull(props);
		Set<Entry<Object, Object>> properties = props.entrySet();
		assertEquals(2, properties.size());
		assertTrue(props.containsKey(JsrJobParametersConverter.JOB_RUN_ID));
		assertEquals("value", props.getProperty("key"));
	}

	@Test
	public void testNonStringJobParameters() {
		JobParameters parameters = new JobParametersBuilder().addLong("key", 5l, false).toJobParameters();
		Properties props = converter.getProperties(parameters);
		assertNotNull(props);
		Set<Entry<Object, Object>> properties = props.entrySet();
		assertEquals(2, properties.size());
		assertTrue(props.containsKey(JsrJobParametersConverter.JOB_RUN_ID));
		assertEquals("5", props.getProperty("key"));
	}

	@Test
	public void testJobParametersWithRunId() {
		JobParameters parameters = new JobParametersBuilder().addLong("key", 5l, false).addLong(JsrJobParametersConverter.JOB_RUN_ID, 2l).toJobParameters();
		Properties props = converter.getProperties(parameters);
		assertNotNull(props);
		Set<Entry<Object, Object>> properties = props.entrySet();
		assertEquals(2, properties.size());
		assertEquals("2", props.getProperty(JsrJobParametersConverter.JOB_RUN_ID));
		assertEquals("5", props.getProperty("key"));
	}

	@Test
	public void testNullProperties() {
		JobParameters parameters = converter.getJobParameters((Properties)null);
		assertNotNull(parameters);
		assertEquals(1, parameters.getParameters().size());
		assertTrue(parameters.getParameters().containsKey(JsrJobParametersConverter.JOB_RUN_ID));
	}

	@Test
	public void testProperties() {
		Properties properties = new Properties();
		properties.put("key", "value");
		JobParameters parameters = converter.getJobParameters(properties);
		assertEquals(2, parameters.getParameters().size());
		assertEquals("value", parameters.getString("key"));
		assertTrue(parameters.getParameters().containsKey(JsrJobParametersConverter.JOB_RUN_ID));
	}

	@Test
	public void testPropertiesWithRunId() {
		Properties properties = new Properties();
		properties.put("key", "value");
		properties.put(JsrJobParametersConverter.JOB_RUN_ID, "3");
		JobParameters parameters = converter.getJobParameters(properties);
		assertEquals(2, parameters.getParameters().size());
		assertEquals("value", parameters.getString("key"));
		assertEquals(Long.valueOf(3l), parameters.getLong(JsrJobParametersConverter.JOB_RUN_ID));
		assertTrue(parameters.getParameters().get(JsrJobParametersConverter.JOB_RUN_ID).isIdentifying());
	}
}
