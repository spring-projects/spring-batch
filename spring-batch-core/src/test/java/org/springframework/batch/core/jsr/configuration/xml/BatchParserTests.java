package org.springframework.batch.core.jsr.configuration.xml;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.configuration.xml.DummyItemProcessor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(value="batch.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class BatchParserTests {

	@Autowired
	@Qualifier("itemProcessor")
	@SuppressWarnings("rawtypes")
	private ItemProcessor itemProcessor;

	@Test
	public void testRoseyScenario() {
		assertNotNull(itemProcessor);
		assertTrue(itemProcessor instanceof PassThroughItemProcessor);
	}

	@Test
	@SuppressWarnings({"resource", "rawtypes"})
	public void testOverrideBeansFirst() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("/org/springframework/batch/core/jsr/configuration/xml/override_batch.xml",
				"/org/springframework/batch/core/jsr/configuration/xml/batch.xml");

		ItemProcessor processor = (ItemProcessor) context.getBean("itemProcessor");

		assertNotNull(processor);
		assertTrue(processor instanceof DummyItemProcessor);
	}

	@Test
	@SuppressWarnings({"resource", "rawtypes"})
	public void testOverrideBeansLast() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("/org/springframework/batch/core/jsr/configuration/xml/batch.xml",
				"/org/springframework/batch/core/jsr/configuration/xml/override_batch.xml");

		ItemProcessor processor = (ItemProcessor) context.getBean("itemProcessor");

		assertNotNull(processor);
		assertTrue(processor instanceof DummyItemProcessor);
	}
}
