/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.batch.item.xml.stax;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.events.XMLEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for {@link UnclosedElementCollectingEventWriter}
 * 
 * @author Jimmy Praet
 */
public class UnclosedElementCollectingEventWriterTests {

	private UnclosedElementCollectingEventWriter writer;

	private XMLEventWriter wrappedWriter;
	
	private XMLEventFactory eventFactory = XMLEventFactory.newInstance();
	
	private QName elementA = new QName("elementA");
	
	private QName elementB = new QName("elementB");
	
	private QName elementC = new QName("elementC");
	
    @Before
	public void setUp() throws Exception {
		wrappedWriter = mock(XMLEventWriter.class);
		writer = new UnclosedElementCollectingEventWriter(wrappedWriter);
	}
    
    @Test
    public void testNoUnclosedElements() throws Exception {
    	writer.add(eventFactory.createStartElement(elementA, null, null));
    	writer.add(eventFactory.createEndElement(elementA, null));

    	assertEquals(0, writer.getUnclosedElements().size());
		verify(wrappedWriter, Mockito.times(2)).add(Mockito.any(XMLEvent.class));
    }

    @Test
    public void testSingleUnclosedElement() throws Exception {
    	writer.add(eventFactory.createStartElement(elementA, null, null));
    	writer.add(eventFactory.createEndElement(elementA, null));
    	writer.add(eventFactory.createStartElement(elementB, null, null));

    	assertEquals(1, writer.getUnclosedElements().size());
    	assertEquals(elementB, writer.getUnclosedElements().get(0));
		verify(wrappedWriter, Mockito.times(3)).add(Mockito.any(XMLEvent.class));
    }

    @Test
    public void testMultipleUnclosedElements() throws Exception {
    	writer.add(eventFactory.createStartElement(elementA, null, null));
    	writer.add(eventFactory.createStartElement(elementB, null, null));
    	writer.add(eventFactory.createStartElement(elementC, null, null));
    	writer.add(eventFactory.createEndElement(elementC, null));

    	assertEquals(2, writer.getUnclosedElements().size());
    	assertEquals(elementA, writer.getUnclosedElements().get(0));
    	assertEquals(elementB, writer.getUnclosedElements().get(1));
		verify(wrappedWriter, Mockito.times(4)).add(Mockito.any(XMLEvent.class));
    }
    
    @Test
    public void testMultipleIdenticalUnclosedElement() throws Exception {
    	writer.add(eventFactory.createStartElement(elementA, null, null));
    	writer.add(eventFactory.createStartElement(elementA, null, null));

    	assertEquals(2, writer.getUnclosedElements().size());
    	assertEquals(elementA, writer.getUnclosedElements().get(0));
    	assertEquals(elementA, writer.getUnclosedElements().get(1));
		verify(wrappedWriter, Mockito.times(2)).add(Mockito.any(XMLEvent.class));
    }    

}
