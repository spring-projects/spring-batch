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

package org.springframework.batch.samples.file.multiline;

import org.jspecify.annotations.Nullable;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.util.Assert;

/**
 * @author Dan Garrette
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
public class MultiLineTradeItemReader implements ItemReader<Trade>, ItemStream {

	private FlatFileItemReader<FieldSet> delegate;

	/**
	 * @see org.springframework.batch.item.ItemReader#read()
	 */
	@Override
	public @Nullable Trade read() throws Exception {
		Trade t = null;

		for (FieldSet line; (line = this.delegate.read()) != null;) {
			String prefix = line.readString(0);
			switch (prefix) {
				case "BEGIN" -> t = new Trade(); // Record must start with 'BEGIN'
				case "INFO" -> {
					Assert.notNull(t, "No 'BEGIN' was found.");
					t.setIsin(line.readString(1));
					t.setCustomer(line.readString(2));
				}
				case "AMNT" -> {
					Assert.notNull(t, "No 'BEGIN' was found.");
					t.setQuantity(line.readInt(1));
					t.setPrice(line.readBigDecimal(2));
				}
				case "END" -> {
					return t; // Record must end with 'END'
				}
			}
		}
		Assert.isNull(t, "No 'END' was found.");
		return null;
	}

	public void setDelegate(FlatFileItemReader<FieldSet> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void close() throws ItemStreamException {
		this.delegate.close();
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		this.delegate.open(executionContext);
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		this.delegate.update(executionContext);
	}

}
