/*
 * Copyright 2010-2023 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.matchers.CompareMatcher;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.xml.domain.Trade;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.oxm.Marshaller;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;
import org.springframework.util.StopWatch;

import static org.hamcrest.MatcherAssert.assertThat;

abstract class AbstractStaxEventWriterItemWriterTests {

	private final Log logger = LogFactory.getLog(getClass());

	private static final int MAX_WRITE = 100;

	protected StaxEventItemWriter<Trade> writer;

	private WritableResource resource;

	private File outputFile;

	protected Resource expected = new ClassPathResource("expected-output.xml", getClass());

	protected Chunk<Trade> objects = Chunk.of(new Trade("isin1", 1, new BigDecimal(1.0), "customer1"),
			new Trade("isin2", 2, new BigDecimal(2.0), "customer2"),
			new Trade("isin3", 3, new BigDecimal(3.0), "customer3"));

	/**
	 * Write list of domain objects and check the output file.
	 */
	@Test
	void testWrite() throws Exception {
		StopWatch stopWatch = new StopWatch(getClass().getSimpleName());
		stopWatch.start();
		for (int i = 0; i < MAX_WRITE; i++) {
			new TransactionTemplate(new ResourcelessTransactionManager())
				.execute((TransactionCallback<Void>) status -> {
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
				});
		}
		writer.close();
		stopWatch.stop();
		logger.info("Timing for XML writer: " + stopWatch);

		assertThat(Input.from(expected.getFile()), CompareMatcher.isSimilarTo(Input.from(resource.getFile()))
			.withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndText)));
	}

	@BeforeEach
	void setUp() throws Exception {
		File directory = new File("target/data");
		directory.mkdirs();
		outputFile = File.createTempFile(ClassUtils.getShortName(this.getClass()), ".xml", directory);
		resource = new FileSystemResource(outputFile);
		writer = new StaxEventItemWriter<>(getMarshaller());
		writer.setResource(resource);

		writer.afterPropertiesSet();

		writer.open(new ExecutionContext());

	}

	@AfterEach
	void tearDown() {
		outputFile.delete();
	}

	/**
	 * @return Marshaller specific for the OXM technology being used.
	 */
	protected abstract Marshaller getMarshaller() throws Exception;

}
