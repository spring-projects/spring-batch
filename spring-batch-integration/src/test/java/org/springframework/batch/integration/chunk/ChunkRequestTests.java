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
package org.springframework.batch.integration.chunk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.springframework.batch.test.MetaDataInstanceFactory;

/**
 * @author Dave Syer
 * 
 */
public class ChunkRequestTests {

	private ChunkRequest<String> request = new ChunkRequest<String>(0, Arrays.asList("foo", "bar"),
			111L, MetaDataInstanceFactory.createStepExecution().createStepContribution());

	@Test
	public void testGetJobId() {
		assertEquals(111L, request.getJobId());
	}

	@Test
	public void testGetItems() {
		assertEquals(2, request.getItems().size());
	}

	@Test
	public void testGetStepContribution() {
		assertNotNull(request.getStepContribution());
	}

	@Test
	public void testToString() {
		System.err.println(request.toString());
	}

	@Test
	public void testSerializable() throws Exception {
		@SuppressWarnings("unchecked")
		ChunkRequest<String> result = (ChunkRequest<String>) SerializationUtils.deserialize(SerializationUtils
				.serialize(request));
		assertNotNull(result.getStepContribution());
		assertEquals(111L, result.getJobId());
		assertEquals(2, result.getItems().size());
	}

}
