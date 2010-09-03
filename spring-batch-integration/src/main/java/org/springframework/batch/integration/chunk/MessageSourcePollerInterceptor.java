package org.springframework.batch.integration.chunk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.channel.interceptor.ChannelInterceptorAdapter;
import org.springframework.integration.core.MessageSource;
import org.springframework.util.Assert;


/**
 * A {@link ChannelInterceptor} that turns a pollable channel into a "pass-thru channel": if a client calls
 * <code>receive()</code> on the channel it will delegate to a {@link MessageSource} to pull the message directly from
 * an external source. This is particularly useful in combination with a message channel in thread scope, in which case
 * the <code>receive()</code> can join a transaction which was started by the caller.
 * 
 * @author Dave Syer
 * 
 */
public class MessageSourcePollerInterceptor extends ChannelInterceptorAdapter implements InitializingBean {

	private static Log logger = LogFactory.getLog(MessageSourcePollerInterceptor.class);

	private MessageSource<?> source;

	private MessageChannel channel;

	/**
	 * Convenient default constructor for configuration purposes.
	 */
	public MessageSourcePollerInterceptor() {
	}

	/**
	 * @param source a message source to poll for messages on receive.
	 */
	public MessageSourcePollerInterceptor(MessageSource<?> source) {
		this.source = source;
	}

	/**
	 * Optional MessageChannel for injecting the message received from the source (defaults to the channel intercepted
	 * in {@link #preReceive(MessageChannel)}).
	 * 
	 * @param channel the channel to set
	 */
	public void setChannel(MessageChannel channel) {
		this.channel = channel;
	}

	/**
	 * Asserts that mandatory properties are set.
	 * @see InitializingBean#afterPropertiesSet()
	 */
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
	 * Receive from the {@link MessageSource} and send immediately to the input channel, so that the call that we are
	 * intercepting always a message to receive.
	 * 
	 * @see ChannelInterceptorAdapter#preReceive(MessageChannel)
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
