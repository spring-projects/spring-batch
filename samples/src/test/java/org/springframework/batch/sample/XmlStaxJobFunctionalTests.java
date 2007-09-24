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

import org.springframework.batch.sample.AbstractLifecycleSpringContextTests;

public class XmlStaxJobFunctionalTests extends AbstractLifecycleSpringContextTests {
	
//	@Override
	protected String[] getConfigLocations() {
		return new String[]{"jobs/xmlStaxJob.xml"};
	}

	/**
	 * Output should be the same as input
	 */
	protected void validatePostConditions() throws Exception {
		applicationContext.close(); //make sure the output file is closed
	
		/* TODO compare DOM to the expected output -> does not work as expected yet, fails on correct output
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document expectedOutput = builder.parse( 
				new File("src/main/resources/data/staxJob/input/20070918.testStream.xmlFileStep.output.xml"));
		
		Document actualOutput = builder.parse(
				new File("20070918.testStream.xmlFileStep.output.xml"));
	
		expectedOutput.normalize();
		actualOutput.normalize();
		
		assertTrue(expectedOutput.isEqualNode(actualOutput));
		*/
	}

}
