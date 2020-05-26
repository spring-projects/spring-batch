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

package org.springframework.batch.item.amqp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * <p>
 * Test cases around {@link AmqpItemReader}.
 * </p>
 *
 * @author Chris Schaefer
 * @author Will Schipp
 */
public class AmqpItemReaderTests {
    @Test(expected = IllegalArgumentException.class)
    public void testNullAmqpTemplate() {
        new AmqpItemReader<String>(null);
    }

    @Test
    public void testNoItemType() {
        final AmqpTemplate amqpTemplate = mock(AmqpTemplate.class);
        when(amqpTemplate.receiveAndConvert()).thenReturn("foo");

        final AmqpItemReader<String> amqpItemReader = new AmqpItemReader<>(amqpTemplate);
        assertEquals("foo", amqpItemReader.read());
    }

    @Test
    public void testNonMessageItemType() {
        final AmqpTemplate amqpTemplate = mock(AmqpTemplate.class);
        when(amqpTemplate.receiveAndConvert()).thenReturn("foo");

        final AmqpItemReader<String> amqpItemReader = new AmqpItemReader<>(amqpTemplate);
        amqpItemReader.setItemType(String.class);

        assertEquals("foo", amqpItemReader.read());

    }

    @Test
    public void testMessageItemType() {
        final AmqpTemplate amqpTemplate = mock(AmqpTemplate.class);
        final Message message = mock(Message.class);

        when(amqpTemplate.receive()).thenReturn(message);

        final AmqpItemReader<Message> amqpItemReader = new AmqpItemReader<>(amqpTemplate);
        amqpItemReader.setItemType(Message.class);

        assertEquals(message, amqpItemReader.read());

    }

    @Test
    public void testTypeMismatch() {
        final AmqpTemplate amqpTemplate = mock(AmqpTemplate.class);

        when(amqpTemplate.receiveAndConvert()).thenReturn("foo");

        final AmqpItemReader<Integer> amqpItemReader = new AmqpItemReader<>(amqpTemplate);
        amqpItemReader.setItemType(Integer.class);

        try {
            amqpItemReader.read();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("wrong type"));
        }

    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullItemType() {
        final AmqpTemplate amqpTemplate = mock(AmqpTemplate.class);

        final AmqpItemReader<String> amqpItemReader = new AmqpItemReader<>(amqpTemplate);
        amqpItemReader.setItemType(null);
    }
}
