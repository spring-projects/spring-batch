/*
 * Copyright 2017 the original author or authors.
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

import org.springframework.batch.item.support.ScriptItemProcessor;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Creates a fully qualified ScriptItemProcessor.
 *
 * @author Glenn Renfro
 *
 * @since 4.0
 */
public class ScriptItemProcessorBuilder<I, O> {

	private String language;

	private Resource scriptResource;

	private String scriptSource;

	private String itemBindingVariableName;

	/**
	 * Sets the {@link org.springframework.core.io.Resource} location of the script to
	 * use. The script language will be deduced from the filename extension.
	 *
	 * @param resource the {@link org.springframework.core.io.Resource} location of the
	 * script to use.
	 * @return this instance for method chaining
	 * @see ScriptItemProcessor#setScript(Resource)
	 * 
	 */
	public ScriptItemProcessorBuilder<I, O> scriptResource(Resource resource) {
		this.scriptResource = resource;

		return this;
	}

	/**
	 * Establishes the language of the script.
	 *
	 * @param language the language of the script.
	 * @return this instance for method chaining
	 * @see ScriptItemProcessor#setScriptSource(String, String)
	 */
	public ScriptItemProcessorBuilder<I, O> language(String language) {
		this.language = language;

		return this;
	}

	/**
	 * Sets the provided {@link String} as the script source code to use. Language must
	 * not be null nor empty when using script.
	 *
	 * @param scriptSource the {@link String} form of the script source code to use.
	 * @return this instance for method chaining
	 * @see ScriptItemProcessor#setScriptSource(String, String)
	 */
	public ScriptItemProcessorBuilder<I, O> scriptSource(String scriptSource) {
		this.scriptSource = scriptSource;

		return this;
	}

	/**
	 * Provides the ability to change the key name that scripts use to obtain the current
	 * item to process if the variable represented by:
	 * {@link ScriptItemProcessor#ITEM_BINDING_VARIABLE_NAME}
	 * is not suitable ("item").
	 *
	 * @param itemBindingVariableName the desired binding variable name
	 * @return this instance for method chaining
	 * @see ScriptItemProcessor#setItemBindingVariableName(String)
	 */
	public ScriptItemProcessorBuilder<I, O> itemBindingVariableName(String itemBindingVariableName) {
		this.itemBindingVariableName = itemBindingVariableName;

		return this;
	}

	/**
	 * Returns a fully constructed {@link ScriptItemProcessor}.
	 *
	 * @return a new {@link ScriptItemProcessor}
	 */
	public ScriptItemProcessor<I, O> build() {
		if (this.scriptResource == null && !StringUtils.hasText(this.scriptSource)) {
			throw new IllegalArgumentException("scriptResource or scriptSource is required.");
		}

		if (StringUtils.hasText(this.scriptSource)) {
			Assert.hasText(this.language, "language is required when using scriptSource.");
		}

		ScriptItemProcessor<I, O> processor = new ScriptItemProcessor<>();
		if (StringUtils.hasText(this.itemBindingVariableName)) {
			processor.setItemBindingVariableName(this.itemBindingVariableName);
		}

		if (this.scriptResource != null) {
			processor.setScript(this.scriptResource);
		}

		if (this.scriptSource != null) {
			processor.setScriptSource(this.scriptSource, this.language);
		}

		return processor;
	}
}
