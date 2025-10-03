/*
 * Copyright 2010-2023 the original author or authors.
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
package org.springframework.batch.integration.chunk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.util.Assert;

/**
 * A {@link ChannelInterceptor} that turns a pollable channel into a "pass-thru channel":
 * if a client calls <code>receive()</code> on the channel it will delegate to a
 * {@link MessageSource} to pull the message directly from an external source. This is
 * particularly useful in combination with a message channel in thread scope, in which
 * case the <code>receive()</code> can join a transaction which was started by the caller.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class MessageSourcePollerInterceptor implements ChannelInterceptor, InitializingBean {

	private static final Log logger = LogFactory.getLog(MessageSourcePollerInterceptor.class);

	private MessageSource<?> source;

	private @Nullable MessageChannel channel;

	/**
	 * @param source a message source to poll for messages on receive.
	 */
	public MessageSourcePollerInterceptor(MessageSource<?> source) {
		this.source = source;
	}

	/**
	 * Optional MessageChannel for injecting the message received from the source
	 * (defaults to the channel intercepted in {@link #preReceive(MessageChannel)}).
	 * @param channel the channel to set
	 */
	public void setChannel(MessageChannel channel) {
		this.channel = channel;
	}

	/**
	 * Asserts that mandatory properties are set.
	 * @see InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(source != null, "A MessageSource must be provided");
	}

	/**
	 * @param source a message source to poll for messages on receive.
	 */
	public void setMessageSource(MessageSource<?> source) {
		this.source = source;
	}

	/**
	 * Receive from the {@link MessageSource} and send immediately to the input channel,
	 * so that the call that we are intercepting always a message to receive.
	 *
	 * @see ChannelInterceptor#preReceive(MessageChannel)
	 */
	@Override
	public boolean preReceive(MessageChannel channel) {
		Message<?> message = source.receive();
		if (message != null) {
			if (this.channel != null) {
				channel = this.channel;
			}
			channel.send(message);
			if (logger.isDebugEnabled()) {
				logger.debug("Sent " + message + " to channel " + channel);
			}
			return true;
		}
		return true;
	}

}
