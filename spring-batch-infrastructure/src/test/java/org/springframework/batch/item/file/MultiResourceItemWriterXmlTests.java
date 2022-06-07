/*
 * Copyright 2009-2022 the original author or authors.
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
package org.springframework.batch.item.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.transform.Result;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.batch.item.xml.StaxTestUtils;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.util.Assert;

/**
 * Tests for {@link MultiResourceItemWriter} delegating to {@link StaxEventItemWriter}.
 */
public class MultiResourceItemWriterXmlTests extends AbstractMultiResourceItemWriterTests {

	final static private String xmlDocStart = "<root>";

	final static private String xmlDocEnd = "</root>";

	private StaxEventItemWriter<String> delegate;

	@BeforeEach
	public void setUp() throws Exception {
		super.createFile();
		delegate = new StaxEventItemWriter<>();
		delegate.setMarshaller(new SimpleMarshaller());
	}

	/**
	 * Writes object's toString representation as tag.
	 */
	private static class SimpleMarshaller implements Marshaller {

		@Override
		public void marshal(Object graph, Result result) throws XmlMappingException, IOException {
			Assert.isInstanceOf(Result.class, result);

			try {
				XMLEventFactory factory = XMLEventFactory.newInstance();
				XMLEventWriter writer = StaxTestUtils.getXmlEventWriter(result);
				writer.add(factory.createStartDocument("UTF-8"));
				writer.add(factory.createStartElement("prefix", "namespace", graph.toString()));
				writer.add(factory.createEndElement("prefix", "namespace", graph.toString()));
				writer.add(factory.createEndDocument());
			}
			catch (Exception e) {
				throw new RuntimeException("Exception while writing to output file", e);
			}
		}

		@Override
		public boolean supports(Class<?> clazz) {
			return true;
		}

	}

	@Override
	protected String readFile(File f) throws Exception {
		String content = super.readFile(f);
		// skip the <?xml ... ?> header to avoid platform issues with single vs. double
		// quotes
		return content.substring(content.indexOf("?>") + 2);
	}

	@Test
	public void multiResourceWritingWithRestart() throws Exception {

		super.setUp(delegate);
		tested.open(executionContext);

		tested.write(Arrays.asList("1", "2", "3"));

		File part1 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(1));
		assertTrue(part1.exists());

		tested.write(Arrays.asList("4"));
		File part2 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(2));
		assertTrue(part2.exists());

		tested.update(executionContext);
		tested.close();

		assertEquals(xmlDocStart + "<prefix:4/>" + xmlDocEnd, readFile(part2));
		assertEquals(xmlDocStart + "<prefix:1/><prefix:2/><prefix:3/>" + xmlDocEnd, readFile(part1));

		tested.open(executionContext);

		tested.write(Arrays.asList("5"));

		tested.write(Arrays.asList("6", "7", "8", "9"));
		File part3 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(3));
		assertTrue(part3.exists());

		tested.close();

		assertEquals(xmlDocStart + "<prefix:4/><prefix:5/>" + xmlDocEnd, readFile(part2));
		assertEquals(xmlDocStart + "<prefix:6/><prefix:7/><prefix:8/><prefix:9/>" + xmlDocEnd, readFile(part3));
	}

}
