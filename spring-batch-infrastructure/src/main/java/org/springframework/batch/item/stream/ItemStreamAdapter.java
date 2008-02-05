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
package org.springframework.batch.item.stream;

import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ExecutionAttributes;
import org.springframework.batch.item.StreamException;

/**
 * @author Dave Syer
 *
 */
public class ItemStreamAdapter implements ItemStream {

	/**
	 * No-op.
	 * @see org.springframework.batch.item.ItemStream#close()
	 */
	public void close() throws StreamException {
	}

	/**
	 * No-op.
	 * @see org.springframework.batch.item.ItemStream#open()
	 */
	public void open() throws StreamException {
	}

	/**
	 * No-op.
	 * @see org.springframework.batch.item.ItemStream#restoreFrom(org.springframework.batch.item.ExecutionAttributes)
	 */
	public void restoreFrom(ExecutionAttributes context) {
	}

	/**
	 * Return empty {@link ExecutionAttributes}.
	 * @see org.springframework.batch.item.ExecutionAttributesProvider#getExecutionAttributes()
	 */
	public ExecutionAttributes getExecutionAttributes() {
		return new ExecutionAttributes();
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#isMarkSupported()
	 */
	public boolean isMarkSupported() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#mark(org.springframework.batch.item.ExecutionAttributes)
	 */
	public void mark(ExecutionAttributes executionAttributes) {
		throw new UnsupportedOperationException("Mark operation not supported.");
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#reset(org.springframework.batch.item.ExecutionAttributes)
	 */
	public void reset(ExecutionAttributes executionAttributes) {
		throw new UnsupportedOperationException("Reset operation not supported.");
	}

}
