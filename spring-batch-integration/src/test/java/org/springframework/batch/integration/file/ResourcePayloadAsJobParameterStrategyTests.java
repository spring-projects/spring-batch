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
package org.springframework.batch.integration.file;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.integration.file.ResourcePayloadAsJobParameterStrategy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Dave Syer
 *
 */
public class ResourcePayloadAsJobParameterStrategyTests {

	/**
	 * 
	 */
	private static final String INPUT_FILE_PATH = ResourcePayloadAsJobParameterStrategy.FILE_INPUT_PATH;

	/**
	 * Test method for {@link org.springframework.batch.integration.file.ResourcePayloadAsJobParameterStrategy#getJobParameters(org.springframework.integration.message.Message)}.
	 */
	@Test
	public void testGetJobParameters() {
		ResourcePayloadAsJobParameterStrategy strategy = new ResourcePayloadAsJobParameterStrategy();
		JobParameters parameters  = strategy.getJobParameters(new GenericMessage<Resource>(new ClassPathResource("log4j.properties")));
		assertTrue(parameters.getParameters().containsKey(INPUT_FILE_PATH));
	}

	/**
	 * Test method for {@link org.springframework.batch.integration.file.ResourcePayloadAsJobParameterStrategy#getJobParameters(org.springframework.integration.message.Message)}.
	 */
	@Test
	public void testGetJobParametersWithWrongPayload() {
		ResourcePayloadAsJobParameterStrategy strategy = new ResourcePayloadAsJobParameterStrategy();
		try {
			strategy.getJobParameters(new GenericMessage<String>("log4j.properties"));
			fail("Expected ClassCastException");
		} catch (ClassCastException e) {
			String message = e.getMessage();
			assertTrue("Wrong message: "+message, message.contains("String cannot be cast"));
		}
		
	}

}
