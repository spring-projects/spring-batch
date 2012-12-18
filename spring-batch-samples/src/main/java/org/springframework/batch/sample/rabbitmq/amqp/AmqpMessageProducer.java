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
public class AmqpMessageProducer {
    public static final int SEND_MESSAGE_COUNT = 10;
    public static final String[] BEAN_CONFIG = { "classpath:/META-INF/spring/jobs/messaging/rabbitmq-beans.xml",
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
