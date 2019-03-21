/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.batch.sample.rabbitmq.amqp;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * <p>
 * Simple producer class that sends {@link String} messages to the configured queue to be processed.
 * </p>
 */
public final class AmqpMessageProducer {
	private AmqpMessageProducer() {}

    private static final int SEND_MESSAGE_COUNT = 10;
    private static final String[] BEAN_CONFIG = { "classpath:/META-INF/spring/jobs/messaging/rabbitmq-beans.xml",
            "classpath:/META-INF/spring/config-beans.xml" };

    public static void main(String[] args) {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(BEAN_CONFIG);
        AmqpTemplate amqpTemplate = applicationContext.getBean("inboundAmqpTemplate", RabbitTemplate.class);

        for (int i = 0; i < SEND_MESSAGE_COUNT; i++ ) {
            amqpTemplate.convertAndSend("foo message: " + i);
        }

        ((ConfigurableApplicationContext) applicationContext).close();
    }
}
