/*
 * Copyright 2010-2019 the original author or authors.
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
package org.springframework.batch.item.xml;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.xml.domain.Trade;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Marshaller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;
import org.springframework.util.StopWatch;

import static org.junit.Assert.assertThat;

public abstract class AbstractStaxEventWriterItemWriterTests {
	
	private Log logger = LogFactory.getLog(getClass());

	private static final int MAX_WRITE = 100;

	protected StaxEventItemWriter<Trade> writer = new StaxEventItemWriter<>();

	private Resource resource;

	private File outputFile;

	protected Resource expected = new ClassPathResource("expected-output.xml", getClass());

	@SuppressWarnings("serial")
	protected List<Trade> objects = new ArrayList<Trade>() {
		{
			add(new Trade("isin1", 1, new BigDecimal(1.0), "customer1"));
			add(new Trade("isin2", 2, new BigDecimal(2.0), "customer2"));
			add(new Trade("isin3", 3, new BigDecimal(3.0), "customer3"));
		}
	};

	/**
	 * Write list of domain objects and check the output file.
	 */
	@SuppressWarnings("resource")
	@Test
	public void testWrite() throws Exception {
		StopWatch stopWatch = new StopWatch(getClass().getSimpleName());
		stopWatch.start();
		for (int i = 0; i < MAX_WRITE; i++) {
			new TransactionTemplate(new ResourcelessTransactionManager()).execute(new TransactionCallback<Void>() {
				@Override
				public Void doInTransaction(TransactionStatus status) {
					try {
						writer.write(objects);
					}
					catch (RuntimeException e) {
						throw e;
					}
					catch (Exception e) {
						throw new IllegalStateException("Exception encountered on write", e);
					}
					return null;
				}
			});
		}
		writer.close();
		stopWatch.stop();
		logger.info("Timing for XML writer: " + stopWatch);

		assertThat(
				Input.from(expected.getFile()),
				CompareMatcher.isSimilarTo(Input.from(resource.getFile()))
						.withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)));
	}

	@Before
	public void setUp() throws Exception {

		File directory = new File("target/data");
		directory.mkdirs();
		outputFile = File.createTempFile(ClassUtils.getShortName(this.getClass()), ".xml", directory);
		resource = new FileSystemResource(outputFile);
		writer.setResource(resource);

		writer.setMarshaller(getMarshaller());
		writer.afterPropertiesSet();

		writer.open(new ExecutionContext());

	}

	@After
	public void tearDown() throws Exception {
		outputFile.delete();
	}

	/**
	 * @return Marshaller specific for the OXM technology being used.
	 */
	protected abstract Marshaller getMarshaller() throws Exception;

}
