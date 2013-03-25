package org.springframework.batch.item.jms;

import javax.jms.Message;

import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.Assert;

public class StatefulJmsItemReader<T> extends
		AbstractItemCountingItemStreamItemReader<T> implements InitializingBean {
	private JmsOperations jmsTemplate;

	private Class<? extends T> itemType;

	@SuppressWarnings("unchecked")
	@Override
	protected T doRead() throws Exception {
		if (itemType != null && itemType.isAssignableFrom(Message.class)) {
			return (T) jmsTemplate.receive();
		}
		Object result = jmsTemplate.receiveAndConvert();
		if (itemType != null && result != null) {
			Assert.state(itemType.isAssignableFrom(result.getClass()),
					"Received message payload of wrong type: expected ["
							+ itemType + "]");
		}
		return (T) result;
	}

	@Override
	protected void doOpen() throws Exception {
		// no op method
	}

	@Override
	protected void doClose() throws Exception {
		// no op method
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jmsTemplate);
		if (jmsTemplate instanceof JmsTemplate) {
			JmsTemplate template = (JmsTemplate) jmsTemplate;
			Assert.isTrue(
					template.getReceiveTimeout() != JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT,
					"JmsTemplate must have a receive timeout!");
			Assert.isTrue(
					template.getDefaultDestination() != null
							|| template.getDefaultDestinationName() != null,
					"JmsTemplate must have a defaultDestination or defaultDestinationName!");
		}

	}

	@Override
	protected void jumpToItem(final int itemIndex) throws Exception {
		// do nothing as there is no storing of the JMS messages and nothing to
		// jump to
	}

	public void setJmsTemplate(final JmsOperations jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	public void setItemType(final Class<? extends T> itemType) {
		this.itemType = itemType;
	}
}