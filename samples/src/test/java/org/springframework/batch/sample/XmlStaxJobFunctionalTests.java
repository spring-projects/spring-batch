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


public class XmlStaxJobFunctionalTests extends AbstractValidatingBatchLauncherTests {
	
	private static final String OUTPUT_FILE = "20070918.testStream.xmlFileStep.output.xml";
	private static final String EXPECTED_OUTPUT_FILE = "src/main/resources/data/staxJob/output/expected-output.xml";
	
//	@Override
	protected String[] getConfigLocations() {
		return new String[]{"jobs/xmlStaxJob.xml"};
	}

	/**
	 * Output should be the same as input
	 */
	protected void validatePostConditions() throws Exception {
		applicationContext.close(); //make sure the output file is closed
		
		XMLUnit.setIgnoreWhitespace(true);
		XMLAssert.assertXMLEqual(new FileReader(EXPECTED_OUTPUT_FILE), new FileReader(OUTPUT_FILE));
	}

}
