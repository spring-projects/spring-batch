/*
 * Copyright 2017-2022 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.support.builder;

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.support.ScriptItemProcessor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Glenn Renfro
 */
public class ScriptItemProcessorBuilderTests {

	private static List<String> availableLanguages = new ArrayList<>();

	@BeforeAll
	public static void populateAvailableEngines() {
		List<ScriptEngineFactory> scriptEngineFactories = new ScriptEngineManager().getEngineFactories();

		for (ScriptEngineFactory scriptEngineFactory : scriptEngineFactories) {
			availableLanguages.addAll(scriptEngineFactory.getNames());
		}
	}

	@BeforeEach
	public void setup() {
		assumeTrue(availableLanguages.contains("javascript"));
	}

	@Test
	public void testScriptSource() throws Exception {
		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessorBuilder<String, Object>()
				.scriptSource("item.toUpperCase();").language("javascript").build();
		scriptItemProcessor.afterPropertiesSet();

		assertEquals("AA", scriptItemProcessor.process("aa"), "Incorrect transformed value");
	}

	@Test
	public void testItemBinding() throws Exception {
		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessorBuilder<String, Object>()
				.scriptSource("foo.contains('World');").language("javascript").itemBindingVariableName("foo").build();
		scriptItemProcessor.afterPropertiesSet();

		assertEquals(true, scriptItemProcessor.process("Hello World"), "Incorrect transformed value");
	}

	@Test
	public void testScriptResource() throws Exception {
		Resource resource = new ClassPathResource("org/springframework/batch/item/support/processor-test-simple.js");
		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessorBuilder<String, Object>()
				.scriptResource(resource).build();
		scriptItemProcessor.afterPropertiesSet();

		assertEquals("BB", scriptItemProcessor.process("bb"), "Incorrect transformed value");
	}

	@Test
	public void testNoScriptSourceNorResource() throws Exception {
		validateExceptionMessage(new ScriptItemProcessorBuilder<>(), "scriptResource or scriptSource is required.");
	}

	@Test
	public void testNoScriptSourceLanguage() throws Exception {
		validateExceptionMessage(
				new ScriptItemProcessorBuilder<String, Object>().scriptSource("foo.contains('World');"),
				"language is required when using scriptSource.");

	}

	private void validateExceptionMessage(ScriptItemProcessorBuilder<String, Object> builder, String message) {
		try {
			builder.build();
			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException iae) {
			assertEquals(message, iae.getMessage(),
					"IllegalArgumentException message did not match the expected result.");
		}
	}

}
