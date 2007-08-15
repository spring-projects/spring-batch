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

package org.springframework.batch.repeat.support;

import junit.framework.TestCase;

import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.support.SimpleFlatFileInputSource;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.provider.AbstractFieldSetItemProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Base class for simple tests with small trade data set.
 * 
 * @author Dave Syer
 * 
 */
public abstract class AbstractTradeBatchTests extends TestCase {

	public static final int NUMBER_OF_ITEMS = 5;

	Resource resource = new ClassPathResource("trades.csv", getClass());

	protected TradeProcessor executor = new TradeProcessor();

	protected TradeItemProvider provider;

	protected void setUp() throws Exception {
		super.setUp();
		provider = new TradeItemProvider(resource);
	}

	protected static class TradeItemProvider extends AbstractFieldSetItemProvider {

		protected TradeItemProvider(Resource resource) throws Exception {
			super();
			SimpleFlatFileInputSource template = new SimpleFlatFileInputSource();
			template.setResource(resource);
			template.afterPropertiesSet();
			setSource(template);
		}

		protected Object transform(FieldSet fieldSet) {
			return new Trade(fieldSet);
		}
	}

	protected static class TradeProcessor implements ItemProcessor {
		int count = 0;

		// This has to be synchronized because we are going to test the state
		// (count) at the end of a concurrent batch run.
		public synchronized void process(Object data) {
			count++;
			System.out.println("Executing trade '" + data + "'");
		}
	}

}
