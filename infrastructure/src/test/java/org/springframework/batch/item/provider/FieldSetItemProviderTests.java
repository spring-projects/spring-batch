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

package org.springframework.batch.item.provider;

import junit.framework.TestCase;

import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.support.SimpleFlatFileInputSource;
import org.springframework.core.io.ByteArrayResource;

public class FieldSetItemProviderTests extends TestCase {

	public void testNotOpen() throws Exception {
		TestItemProvider provider = new TestItemProvider("one\ntwo\nthree");
		assertNotNull(provider.next());
	}

	public void testNext() throws Exception {
		TestItemProvider provider = new TestItemProvider("one\ntwo\nthree");
		assertEquals("[one]", provider.next());
		assertEquals("[two]", provider.next());
		assertEquals("[three]", provider.next());
		assertEquals(null, provider.next());
	}

	private static class TestItemProvider extends AbstractFieldSetItemProvider {
		public TestItemProvider(String data) throws Exception {
			super();
			SimpleFlatFileInputSource template = new SimpleFlatFileInputSource();
			template.setResource(new ByteArrayResource(data.getBytes()));
			template.afterPropertiesSet();
			setSource(template);
		}

		protected Object transform(FieldSet fieldSet) {
			return fieldSet.toString();
		}
	}

}
