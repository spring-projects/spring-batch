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

package org.springframework.batch.sample.iosample.internal;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.sample.domain.trade.Trade;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class MultiLineTradeItemWriter implements ItemWriter<Trade>, ItemStream {

	private FlatFileItemWriter<String> delegate;

	public void write(List<? extends Trade> items) throws Exception {
		List<String> lines = new ArrayList<String>();
		for (Trade t : items) {
			lines.add("BEGIN");
			lines.add("INFO," + t.getIsin() + "," + t.getCustomer());
			lines.add("AMNT," + t.getQuantity() + "," + t.getPrice());
			lines.add("END");
		}
		this.delegate.write(lines);
	}

	public void setDelegate(FlatFileItemWriter<String> delegate) {
		this.delegate = delegate;
	}

	public void close() throws ItemStreamException {
		this.delegate.close();
	}

	public void open(ExecutionContext executionContext) throws ItemStreamException {
		this.delegate.open(executionContext);
	}

	public void update(ExecutionContext executionContext) throws ItemStreamException {
		this.delegate.update(executionContext);
	}
}
