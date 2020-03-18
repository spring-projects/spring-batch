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

package org.springframework.batch.item.jms.builder;

import org.springframework.batch.item.jms.JmsItemWriter;
import org.springframework.jms.core.JmsOperations;
import org.springframework.util.Assert;

/**
 * Creates a fully qualified JmsItemWriter.
 *
 * @author Glenn Renfro
 *
 * @since 4.0
 */
public class JmsItemWriterBuilder<T> {

	private JmsOperations jmsTemplate;

	/**
	 * Establish the JMS template that will be used by the {@link JmsItemWriter}.
	 *
	 * @param jmsTemplate a {@link JmsOperations} instance
	 * @return this instance for method chaining.
	 * @see JmsItemWriter#setJmsTemplate(JmsOperations)
	 */
	public JmsItemWriterBuilder<T> jmsTemplate(JmsOperations jmsTemplate) {
		this.jmsTemplate = jmsTemplate;

		return this;
	}

	/**
	 * Returns a fully constructed {@link JmsItemWriter}.
	 *
	 * @return a new {@link JmsItemWriter}
	 */
	public JmsItemWriter<T> build() {
		Assert.notNull(this.jmsTemplate, "jmsTemplate is required.");
		JmsItemWriter<T> jmsItemWriter = new JmsItemWriter<>();

		jmsItemWriter.setJmsTemplate(this.jmsTemplate);
		return jmsItemWriter;
	}
}
