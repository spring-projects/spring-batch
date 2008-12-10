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

package org.springframework.batch.sample.domain.trade.internal;

import java.util.Collections;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.sample.domain.trade.CustomerCreditDao;
import org.springframework.beans.factory.DisposableBean;

/**
 * Writes customer's credit information in a file.
 *
 * @see CustomerCreditDao
 * @author Robert Kasanicky
 */
public class FlatFileCustomerCreditDao implements CustomerCreditDao,
		DisposableBean {

	private ItemWriter<String> itemWriter;

	private String separator = "\t";

	private volatile boolean opened = false;

	public void writeCredit(CustomerCredit customerCredit) throws Exception {

		if (!opened) {
			open(new ExecutionContext());
		}

		String line = "" + customerCredit.getName() + separator
				+ customerCredit.getCredit();

		itemWriter.write(Collections.singletonList(line));
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

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() throws Exception {
		close();
	}

}
