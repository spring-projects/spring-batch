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

import org.easymock.EasyMock;
import org.junit.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Arrays;

/**
 * <p>
 * Test cases around {@link AmqpItemWriter}.
 * </p>
 *
 * @author Chris Schaefer
 */
public class AmqpItemWriterTests {
    @Test(expected = IllegalArgumentException.class)
    public void testNullAmqpTemplate() {
        new AmqpItemWriter<String>(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullConnectionFactory() {
        new AmqpItemWriter<String>(new RabbitTemplate());
    }

    @Test
    public void voidTestWrite() throws Exception {
        AmqpTemplate amqpTemplate = EasyMock.createMock(AmqpTemplate.class);

        amqpTemplate.convertAndSend("foo");
        EasyMock.expectLastCall();

        amqpTemplate.convertAndSend("bar");
        EasyMock.expectLastCall();

        EasyMock.replay(amqpTemplate);

        AmqpItemWriter<String> amqpItemWriter = new AmqpItemWriter<String>(amqpTemplate);
        amqpItemWriter.write(Arrays.asList("foo", "bar"));

        EasyMock.verify(amqpTemplate);

    }
}
