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

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dave Syer
 * 
 */
@ContextConfiguration()
@RunWith(SpringJUnit4ClassRunner.class)
@MessageEndpoint
public class ResourceSplitterIntegrationTests {

	@Autowired
	@Qualifier("resources")
	private MessageChannel resources;

	@Autowired
	@Qualifier("requests")
	private PollableChannel requests;

	/*
	 * This is so cool (but see INT-190)...<br/>
	 * 
	 * The incoming message is a Resource pattern, and it is converted to the
	 * correct payload type with Spring's default strategy
	 */
	@Splitter(inputChannel = "resources", outputChannel = "requests")
	public Resource[] handle(Resource[] message) {
		List<Resource> list = Arrays.asList(message);
		System.err.println(list);
		return message;
	}

	@SuppressWarnings("unchecked")
	@Test
	// This broke with Integration 2.0 in a milestone, so watch out when upgrading...
	public void testVanillaConversion() throws Exception {
		resources.send(new GenericMessage<String>("classpath:*-context.xml"));
		Message<Resource> message = (Message<Resource>) requests.receive(200L);
		assertNotNull(message);
		message = (Message<Resource>) requests.receive(100L);
		assertNotNull(message);
	}

}
