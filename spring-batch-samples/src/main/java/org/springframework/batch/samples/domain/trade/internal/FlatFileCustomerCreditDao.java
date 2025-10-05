/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.samples.domain.trade.internal;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStream;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.samples.domain.trade.CustomerCredit;
import org.springframework.batch.samples.domain.trade.CustomerCreditDao;
import org.springframework.beans.factory.DisposableBean;

/**
 * Writes customer's credit information in a file.
 *
 * @see CustomerCreditDao
 * @author Robert Kasanicky
 * @author Mahmoud Ben Hassine
 */
public class FlatFileCustomerCreditDao implements CustomerCreditDao, DisposableBean {

	private ItemWriter<String> itemWriter;

	private String separator = "\t";

	private volatile boolean opened = false;

	@Override
	public void writeCredit(CustomerCredit customerCredit) throws Exception {

		if (!opened) {
			open(new ExecutionContext());
		}

		String line = customerCredit.getName() + separator + customerCredit.getCredit();

		itemWriter.write(Chunk.of(line));
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public void setItemWriter(ItemWriter<String> itemWriter) {
		this.itemWriter = itemWriter;
	}

	public void open(ExecutionContext executionContext) throws Exception {
		if (itemWriter instanceof ItemStream) {
			((ItemStream) itemWriter).open(executionContext);
		}
		opened = true;
	}

	public void close() throws Exception {
		if (itemWriter instanceof ItemStream) {
			((ItemStream) itemWriter).close();
		}
	}

	@Override
	public void destroy() throws Exception {
		close();
	}

}
