package org.springframework.batch.sample.rabbitmq.processor;

import org.springframework.batch.item.ItemProcessor;

import java.util.Date;

/**
 * <p>
 * Simple {@link ItemProcessor} implementation to append a "processed on" {@link Date} to a received message.
 * </p>
 */
public class MessageProcessor implements ItemProcessor<String, String> {

    public String process(String message) throws Exception {
        return "Message: \"" + message + "\" processed on: " + new Date();
    }
}
