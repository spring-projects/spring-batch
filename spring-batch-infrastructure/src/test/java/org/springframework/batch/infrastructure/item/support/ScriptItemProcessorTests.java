/*
 * Copyright 2014-2023 the original author or authors.
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
package org.springframework.batch.infrastructure.item.support;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.support.ScriptItemProcessor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.scripting.bsh.BshScriptEvaluator;
import org.springframework.scripting.groovy.GroovyScriptEvaluator;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * <p>
 * Test cases around {@link ScriptItemProcessor}.
 * </p>
 *
 * @author Chris Schaefer
 * @author Mahmoud Ben Hassine
 * @since 3.1
 */
class ScriptItemProcessorTests {

	private static final List<String> availableLanguages = new ArrayList<>();

	@BeforeAll
	static void populateAvailableEngines() {
		List<ScriptEngineFactory> scriptEngineFactories = new ScriptEngineManager().getEngineFactories();

		for (ScriptEngineFactory scriptEngineFactory : scriptEngineFactories) {
			availableLanguages.addAll(scriptEngineFactory.getNames());
		}
	}

	@Test
	void testJavascriptScriptSourceSimple() throws Exception {
		assumeTrue(languageExists("javascript"));

		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessor<>();
		scriptItemProcessor.setScriptSource("item.toUpperCase();", "javascript");
		scriptItemProcessor.afterPropertiesSet();

		assertEquals("SS", scriptItemProcessor.process("ss"), "Incorrect transformed value");
	}

	@Test
	void testJavascriptScriptSourceFunction() throws Exception {
		assumeTrue(languageExists("javascript"));

		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessor<>();
		scriptItemProcessor.setScriptSource("function process(item) { return item.toUpperCase(); } process(item);",
				"javascript");
		scriptItemProcessor.afterPropertiesSet();

		assertEquals("SS", scriptItemProcessor.process("ss"), "Incorrect transformed value");
	}

	@Test
	void testJRubyScriptSourceSimple() throws Exception {
		assumeTrue(languageExists("jruby"));

		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessor<>();
		scriptItemProcessor.setScriptSource("item.upcase", "jruby");
		scriptItemProcessor.afterPropertiesSet();

		assertEquals("SS", scriptItemProcessor.process("ss"), "Incorrect transformed value");
	}

	@Test
	void testJRubyScriptSourceMethod() throws Exception {
		assumeTrue(languageExists("jruby"));

		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessor<>();
		scriptItemProcessor.setScriptSource("def process(item) item.upcase end \n process(item)", "jruby");
		scriptItemProcessor.afterPropertiesSet();

		assertEquals("SS", scriptItemProcessor.process("ss"), "Incorrect transformed value");
	}

	@Test
	void testBeanShellScriptSourceSimple() throws Exception {
		assumeTrue(languageExists("bsh"));

		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessor<>();
		scriptItemProcessor.setScriptSource("item.toUpperCase();", "bsh");
		scriptItemProcessor.afterPropertiesSet();

		assertEquals("SS", scriptItemProcessor.process("ss"), "Incorrect transformed value");
	}

	@Test
	void testBeanShellScriptSourceFunction() throws Exception {
		assumeTrue(languageExists("bsh"));

		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessor<>();
		scriptItemProcessor.setScriptSource("String process(String item) { return item.toUpperCase(); } process(item);",
				"bsh");
		scriptItemProcessor.afterPropertiesSet();

		assertEquals("SS", scriptItemProcessor.process("ss"), "Incorrect transformed value");
	}

	@Test
	void testGroovyScriptSourceSimple() throws Exception {
		assumeTrue(languageExists("groovy"));

		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessor<>();
		scriptItemProcessor.setScriptSource("item.toUpperCase();", "groovy");
		scriptItemProcessor.afterPropertiesSet();

		assertEquals("SS", scriptItemProcessor.process("ss"), "Incorrect transformed value");
	}

	@Test
	void testGroovyScriptSourceMethod() throws Exception {
		assumeTrue(languageExists("groovy"));

		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessor<>();
		scriptItemProcessor.setScriptSource("def process(item) { return item.toUpperCase() } \n process(item)",
				"groovy");
		scriptItemProcessor.afterPropertiesSet();

		assertEquals("SS", scriptItemProcessor.process("ss"), "Incorrect transformed value");
	}

	@Test
	void testJavascriptScriptSimple() throws Exception {
		assumeTrue(languageExists("javascript"));

		Resource resource = new ClassPathResource(
				"org/springframework/batch/infrastructure/item/support/processor-test-simple.js");

		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessor<>();
		scriptItemProcessor.setScript(resource);
		scriptItemProcessor.afterPropertiesSet();

		assertEquals("SS", scriptItemProcessor.process("ss"), "Incorrect transformed value");
	}

	@Test
	void testItemBinding() throws Exception {
		assumeTrue(languageExists("javascript"));

		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessor<>();
		scriptItemProcessor.setScriptSource("foo.contains('World');", "javascript");
		scriptItemProcessor.setItemBindingVariableName("foo");

		scriptItemProcessor.afterPropertiesSet();

		assertEquals(true, scriptItemProcessor.process("Hello World"), "Incorrect transformed value");
	}

	@Test
	void testNoScriptSet() {
		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessor<>();
		assertThrows(IllegalStateException.class, scriptItemProcessor::afterPropertiesSet);
	}

	@Test
	void testScriptSourceAndScriptResourceSet() {
		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessor<>();
		scriptItemProcessor.setScriptSource("blah", "blah");
		scriptItemProcessor.setScript(new ClassPathResource("blah"));
		assertThrows(IllegalStateException.class, scriptItemProcessor::afterPropertiesSet);
	}

	@Test
	void testNoScriptSetWithoutInitBean() {
		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessor<>();
		assertThrows(IllegalStateException.class, () -> scriptItemProcessor.process("blah"));
	}

	@Test
	void testScriptSourceWithNoLanguage() {
		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessor<>();
		assertThrows(IllegalArgumentException.class, () -> scriptItemProcessor
			.setScriptSource("function process(item) { return item.toUpperCase(); } process(item);", null));
	}

	@Test
	void testItemBindingNameChange() throws Exception {
		assumeTrue(languageExists("javascript"));

		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessor<>();
		scriptItemProcessor.setItemBindingVariableName("someOtherVarName");
		scriptItemProcessor.setScriptSource(
				"function process(param) { return param.toUpperCase(); } process(someOtherVarName);", "javascript");
		scriptItemProcessor.afterPropertiesSet();

		assertEquals("SS", scriptItemProcessor.process("ss"), "Incorrect transformed value");
	}

	@Test
	void testBshScriptEvaluator() throws Exception {
		assumeTrue(languageExists("bsh"));

		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessor<>();
		scriptItemProcessor.setScriptEvaluator(new BshScriptEvaluator());
		scriptItemProcessor.setScriptSource("String process(String item) { return item.toUpperCase(); } process(item);",
				"bsh");
		scriptItemProcessor.afterPropertiesSet();

		assertEquals("SS", scriptItemProcessor.process("ss"), "Incorrect transformed value");
	}

	@Test
	void testGroovyScriptEvaluator() throws Exception {
		assumeTrue(languageExists("groovy"));

		ScriptItemProcessor<String, Object> scriptItemProcessor = new ScriptItemProcessor<>();
		scriptItemProcessor.setScriptEvaluator(new GroovyScriptEvaluator());
		scriptItemProcessor.setScriptSource("def process(item) { return item.toUpperCase() } \n process(item)",
				"groovy");
		scriptItemProcessor.afterPropertiesSet();

		assertEquals("SS", scriptItemProcessor.process("ss"), "Incorrect transformed value");
	}

	private boolean languageExists(String engineName) {
		return availableLanguages.contains(engineName);
	}

}
