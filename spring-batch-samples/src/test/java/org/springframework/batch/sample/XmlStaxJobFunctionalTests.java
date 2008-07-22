/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.sample;

import java.io.FileReader;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class XmlStaxJobFunctionalTests extends AbstractValidatingBatchLauncherTests {
	
	private static final String OUTPUT_FILE = "target/test-outputs/20070918.testStream.xmlFileStep.output.xml";
	private static final String EXPECTED_OUTPUT_FILE = "src/main/resources/data/staxJob/output/expected-output.xml";

	/**
	 * Output should be the same as input
	 */
	protected void validatePostConditions() throws Exception {
		XMLUnit.setIgnoreWhitespace(true);
		XMLAssert.assertXMLEqual(new FileReader(EXPECTED_OUTPUT_FILE), new FileReader(OUTPUT_FILE));
	}

}
