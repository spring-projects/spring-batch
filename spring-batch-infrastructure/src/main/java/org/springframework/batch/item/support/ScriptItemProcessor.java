/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.batch.item.support;

import org.springframework.lang.Nullable;
import org.springframework.scripting.support.StaticScriptSource;
import org.springframework.util.StringUtils;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.scripting.ScriptEvaluator;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.scripting.support.StandardScriptEvaluator;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * {@link org.springframework.batch.item.ItemProcessor} implementation that passes the current
 * item to process to the provided script. Exposes the current item for processing via the
 * {@link org.springframework.batch.item.support.ScriptItemProcessor#ITEM_BINDING_VARIABLE_NAME}
 * key name ("item"). A custom key name can be set by invoking:
 * {@link org.springframework.batch.item.support.ScriptItemProcessor#setItemBindingVariableName}
 * with the desired key name. The thread safety of this {@link org.springframework.batch.item.ItemProcessor}
 * depends on the implementation of the {@link org.springframework.scripting.ScriptEvaluator} used.
 * </p>
 *
 *
 * @author Chris Schaefer
 * @since 3.0
 */
public class ScriptItemProcessor<I, O> implements ItemProcessor<I, O>, InitializingBean {
	private static final String ITEM_BINDING_VARIABLE_NAME = "item";

	private String language;
	private ScriptSource script;
	private ScriptSource scriptSource;
	private ScriptEvaluator scriptEvaluator;
	private String itemBindingVariableName = ITEM_BINDING_VARIABLE_NAME;

	@Nullable
	@Override
	@SuppressWarnings("unchecked")
	public O process(I item) throws Exception {
		Map<String, Object> arguments = new HashMap<>();
		arguments.put(itemBindingVariableName, item);

		return (O) scriptEvaluator.evaluate(getScriptSource(), arguments);
	}

	/**
	 * <p>
	 * Sets the {@link org.springframework.core.io.Resource} location of the script to use.
	 * The script language will be deduced from the filename extension.
	 * </p>
	 *
	 * @param resource the {@link org.springframework.core.io.Resource} location of the script to use.
	 */
	public void setScript(Resource resource) {
		Assert.notNull(resource, "The script resource cannot be null");

		this.script = new ResourceScriptSource(resource);
	}

	/**
	 * <p>
	 * Sets the provided {@link String} as the script source code to use.
	 * </p>
	 *
	 * @param scriptSource the {@link String} form of the script source code to use.
	 * @param language the language of the script.
	 */
	public void setScriptSource(String scriptSource, String language) {
		Assert.hasText(language, "Language must contain the script language");
		Assert.hasText(scriptSource, "Script source must contain the script source to evaluate");

		this.language = language;
		this.scriptSource = new StaticScriptSource(scriptSource);
	}

	/**
	 * <p>
	 * Provides the ability to change the key name that scripts use to obtain the current
	 * item to process if the variable represented by:
	 * {@link org.springframework.batch.item.support.ScriptItemProcessor#ITEM_BINDING_VARIABLE_NAME}
	 * is not suitable ("item").
	 * </p>
	 *
	 * @param itemBindingVariableName the desired binding variable name
	 */
	public void setItemBindingVariableName(String itemBindingVariableName) {
		this.itemBindingVariableName = itemBindingVariableName;
	}

	/**
	 * <p>
	 * Provides the ability to set a custom {@link org.springframework.scripting.ScriptEvaluator}
	 * implementation. If not set, a {@link org.springframework.scripting.support.StandardScriptEvaluator}
	 * will be used by default.
	 * </p>
	 *
	 * @param scriptEvaluator the {@link org.springframework.scripting.ScriptEvaluator} to use
	 */
	public void setScriptEvaluator(ScriptEvaluator scriptEvaluator) {
		this.scriptEvaluator = scriptEvaluator;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(scriptEvaluator == null) {
			scriptEvaluator = new StandardScriptEvaluator();
		}

		Assert.state(scriptSource != null || script != null,
				"Either the script source or script file must be provided");

		Assert.state(scriptSource == null || script == null,
				"Either a script source or script file must be provided, not both");

		if (scriptSource != null && scriptEvaluator instanceof StandardScriptEvaluator) {
			Assert.isTrue(!StringUtils.isEmpty(language),
					"Language must be provided when using the default ScriptEvaluator and raw source code");

			((StandardScriptEvaluator) scriptEvaluator).setLanguage(language);
		}
	}

	private ScriptSource getScriptSource() {
		if (script != null) {
			return script;
		}

		if (scriptSource != null) {
			return scriptSource;
		}

		throw new IllegalStateException("Either a script source or script needs to be provided.");
	}
}
