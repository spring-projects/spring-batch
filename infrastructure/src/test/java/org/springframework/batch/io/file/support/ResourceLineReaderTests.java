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

package org.springframework.batch.io.file.support;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.springframework.batch.io.exception.BatchEnvironmentException;
import org.springframework.batch.io.file.support.ResourceLineReader;
import org.springframework.batch.io.file.support.separator.SuffixRecordSeparatorPolicy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

/**
 * @author Rob Harrop
 */
public class ResourceLineReaderTests extends TestCase {

	public void testBadResource() throws Exception {
		ResourceLineReader reader = new ResourceLineReader(new InputStreamResource(new InputStream() {
			public int read() throws IOException {
				throw new IOException("Foo");
			}
		}));
		try {
			reader.read();
			fail("Expected InputException");
		}
		catch (BatchEnvironmentException e) {
			// expected
			assertTrue(e.getMessage().startsWith("Unable to read"));
		}
	}

	public void testRead() throws Exception {
		Resource resource = new ByteArrayResource("a,b,c\n1,2,3".getBytes());
		ResourceLineReader reader = new ResourceLineReader(resource);
		int count = 0;
		String line;
		while ((line = (String) reader.read()) != null) {
			count++;
			assertNotNull(line);
		}

		assertEquals(2, count);
	}
	
	public void testCloseTwice() throws Exception {
		Resource resource = new ByteArrayResource("a,b,c\n1,2,3".getBytes());
		ResourceLineReader reader = new ResourceLineReader(resource);
		reader.open();
		reader.close();
		try {
			reader.close(); // just closing a BufferedReader twice should be fine
		} catch (Exception e) {
			fail("Unexpected Exception "+e);
		}
		assertEquals("a,b,c", reader.read());
	}
	
	public void testEncoding() throws Exception {
		Resource resource = new ByteArrayResource("a,b,c\n1,2,3".getBytes());
		ResourceLineReader reader = new ResourceLineReader(resource, "UTF-8");
		int count = 0;
		String line;
		while ((line = (String) reader.read()) != null) {
			count++;
			assertNotNull(line);
		}

		assertEquals(2, count);		
	}
	
	public void testLineCount() throws Exception {
		Resource resource = new ByteArrayResource("1,2,\"3\n4\"\n5,6,7".getBytes());
		ResourceLineReader reader = new ResourceLineReader(resource);
		reader.read();	
		assertEquals(2, reader.getCurrentLineCount());
	}
	
	public void testLineEndings() throws Exception {
		Resource resource = new ByteArrayResource("1\n2\r\n3".getBytes());
		ResourceLineReader reader = new ResourceLineReader(resource);
		reader.read();
		String line = (String) reader.read();
		assertEquals("2", line);
		assertEquals(2, reader.getCurrentLineCount());
		line = (String) reader.read();
		assertEquals("3", line);
		assertEquals(3, reader.getCurrentLineCount());
	}

	public void testDefaultComments() throws Exception {
		Resource resource = new ByteArrayResource("1\n# 2\n3".getBytes());
		ResourceLineReader reader = new ResourceLineReader(resource);
		reader.read();
		String line = (String) reader.read();
		assertEquals("3", line);		
	}

	public void testComments() throws Exception {
		Resource resource = new ByteArrayResource("1\n-- 2\n3".getBytes());
		ResourceLineReader reader = new ResourceLineReader(resource);
		reader.setComments(new String[] {"//", "--"});
		reader.read();
		String line = (String) reader.read();
		assertEquals("3", line);		
	}

    public void testCommentOnTheLastLine() throws Exception {
        Resource resource = new ByteArrayResource("1\n#last line".getBytes());
        ResourceLineReader reader = new ResourceLineReader(resource);
        reader.read();
        String line = (String) reader.read();
        assertNull(line);        
    }
	
	public void testResetNewReader() throws Exception {
		Resource resource = new ByteArrayResource("1\n4\n5".getBytes());
		ResourceLineReader reader = new ResourceLineReader(resource);
		reader.reset();
		assertEquals(0, reader.getCurrentLineCount());
	}
	
	public void testMarkReset() throws Exception {
		Resource resource = new ByteArrayResource("1\n4\n5".getBytes());
		ResourceLineReader reader = new ResourceLineReader(resource);
		reader.read();	
		assertEquals(1, reader.getCurrentLineCount());
		reader.mark();
		reader.read();	
		assertEquals(2, reader.getCurrentLineCount());
		reader.reset();
		reader.read();	
		assertEquals(2, reader.getCurrentLineCount());
	}
	
	public void testNonDefaultRecordSeparatorPolicy() throws Exception {
		Resource resource = new ByteArrayResource("1\n\"4\n5\"; \n6".getBytes());
		ResourceLineReader reader = new ResourceLineReader(resource);
		reader.setRecordSeparatorPolicy(new SuffixRecordSeparatorPolicy());
		assertEquals(0, reader.getCurrentLineCount());
		String line = (String) reader.read();
		assertEquals("1\"4\n5\"", line);				
	}
}
