/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.batch.samples.amqp;

import org.springframework.batch.item.ItemProcessor;

import java.util.Date;

import org.jspecify.annotations.Nullable;

/**
 * <p>
 * Simple {@link ItemProcessor} implementation to append a "processed on" {@link Date} to
 * a received message.
 * </p>
 */
public class MessageProcessor implements ItemProcessor<String, String> {

	@Override
	public @Nullable String process(String message) throws Exception {
		return "Message: \"" + message + "\" processed on: " + new Date();
	}

}
