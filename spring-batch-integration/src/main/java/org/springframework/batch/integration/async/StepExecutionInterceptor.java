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
package org.springframework.batch.integration.async;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.channel.interceptor.ChannelInterceptorAdapter;
import org.springframework.integration.support.MessageBuilder;

/**
 * A {@link ChannelInterceptor} that adds the current {@link StepExecution} (if
 * there is one) as a header to the message. Downstream asynchronous handlers
 * can then take advantage of the step context without needing to be step
 * scoped, which is a problem for handlers executing in another thread because
 * the scope context is not available.
 * 
 * @author Dave Syer
 * 
 */
public class StepExecutionInterceptor extends ChannelInterceptorAdapter {

	/**
	 * The name of the header
	 */
	public static final String STEP_EXECUTION = "stepExecution";

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StepContext context = StepSynchronizationManager.getContext();
		if (context == null) {
			return message;
		}
		return MessageBuilder.fromMessage(message).setHeader(STEP_EXECUTION, context.getStepExecution()).build();
	}

}
