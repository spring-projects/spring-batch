/*
 * Copyright 2012 the original author or authors.
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

package org.springframework.batch.item.amqp;

import org.easymock.classextension.EasyMock;
import org.junit.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * <p>
 * Test cases around {@link AmqpItemReader}.
 * </p>
 *
 * @author Chris Schaefer
 */
public class AmqpItemReaderTests {
    @Test(expected = IllegalArgumentException.class)
    public void testNullAmqpTemplate() {
        new AmqpItemReader<String>(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullAmqpTemplateConnectionFactory() {
        new AmqpItemReader<String>(new RabbitTemplate());
    }

    @Test
    public void testNoItemType() {
        final AmqpTemplate amqpTemplate = EasyMock.createMock(AmqpTemplate.class);
        EasyMock.expect(amqpTemplate.receiveAndConvert()).andReturn("foo");
        EasyMock.replay(amqpTemplate);

        final AmqpItemReader<String> amqpItemReader = new AmqpItemReader<String>(amqpTemplate);
        assertEquals("foo", amqpItemReader.read());
        EasyMock.verify(amqpTemplate);
    }

    @Test
    public void testNonMessageItemType() {
        final AmqpTemplate amqpTemplate = EasyMock.createMock(AmqpTemplate.class);
        EasyMock.expect(amqpTemplate.receiveAndConvert()).andReturn("foo");
        EasyMock.replay(amqpTemplate);

        final AmqpItemReader<String> amqpItemReader = new AmqpItemReader<String>(amqpTemplate);
        amqpItemReader.setItemType(String.class);

        assertEquals("foo", amqpItemReader.read());

        EasyMock.verify(amqpTemplate);
    }

    @Test
    public void testMessageItemType() {
        final AmqpTemplate amqpTemplate = EasyMock.createMock(AmqpTemplate.class);
        final Message message = EasyMock.createMock(Message.class);

        EasyMock.expect(amqpTemplate.receive()).andReturn(message);
        EasyMock.replay(amqpTemplate, message);

        final AmqpItemReader<Message> amqpItemReader = new AmqpItemReader<Message>(amqpTemplate);
        amqpItemReader.setItemType(Message.class);

        assertEquals(message, amqpItemReader.read());

        EasyMock.verify(amqpTemplate);
    }

    @Test
    public void testTypeMismatch() {
        final AmqpTemplate amqpTemplate = EasyMock.createMock(AmqpTemplate.class);

        EasyMock.expect(amqpTemplate.receiveAndConvert()).andReturn("foo");
        EasyMock.replay(amqpTemplate);

        final AmqpItemReader<Integer> amqpItemReader = new AmqpItemReader<Integer>(amqpTemplate);
        amqpItemReader.setItemType(Integer.class);

        try {
            amqpItemReader.read();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("wrong type"));
        }

        EasyMock.verify(amqpTemplate);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullItemType() {
        final AmqpTemplate amqpTemplate = EasyMock.createMock(AmqpTemplate.class);

        final AmqpItemReader<String> amqpItemReader = new AmqpItemReader<String>(amqpTemplate);
        amqpItemReader.setItemType(null);
    }
}
