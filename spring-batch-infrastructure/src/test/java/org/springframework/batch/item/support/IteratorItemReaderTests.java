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

package org.springframework.batch.item.support;

import java.util.Arrays;

import junit.framework.TestCase;

public class IteratorItemReaderTests extends TestCase{

    public void testIterable() throws Exception {
        IteratorItemReader<String> reader = new IteratorItemReader<String>(Arrays.asList(new String[] { "a", "b", "c" }));
        assertEquals("a", reader.read());
        assertEquals("b", reader.read());
        assertEquals("c", reader.read());
        assertEquals(null, reader.read());
    }

    public void testIterator() throws Exception {
        IteratorItemReader<String> reader = new IteratorItemReader<String>(Arrays.asList(new String[] { "a", "b", "c" }).iterator());
        assertEquals("a", reader.read());
        assertEquals("b", reader.read());
        assertEquals("c", reader.read());
        assertEquals(null, reader.read());
    }

}
