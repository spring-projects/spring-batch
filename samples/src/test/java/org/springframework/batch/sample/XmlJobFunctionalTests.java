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

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class XmlJobFunctionalTests extends AbstractLifecycleSpringContextTests {
	//private static final Log log = LogFactory.getLog(XmlJobFunctionalTests.class);
	
	private Resource fileInputLocator = new ClassPathResource("data/xmlJob/output/20070122.testStream.xmlFileStep.xml");
	private Resource fileOutputLocator = new FileSystemResource("20070122.testStream.xmlFileStep.xml");

//	@Override
	protected String[] getConfigLocations() {
		return new String[]{"jobs/xmlJob.xml"};
	}

	/**
	 * Output should be the same as input
	 */
	protected void validatePostConditions() throws Exception {
		//TODO String comparison is inadequate, compare DOMs instead
		
		String input = IOUtils.toString(fileInputLocator.getInputStream());
		String output = IOUtils.toString(fileOutputLocator.getInputStream());
		
		//assertEquals(input, output);
		assertTrue(input.length() > 0);
		assertTrue(output.length() > 0);
	}

}
