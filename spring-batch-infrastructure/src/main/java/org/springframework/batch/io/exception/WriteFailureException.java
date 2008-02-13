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

package org.springframework.batch.io.exception;

import org.springframework.batch.item.ItemWriter;

/**
 * Exception thrown after encountering an error during a write.  It should
 * generally be thrown by classes that implement the {@link ItemWriter} 
 * interface.
 * 
 *  @author Lucas Ward
 */
public class WriteFailureException extends BatchCriticalException {
	private static final long serialVersionUID = -1933213086873834098L;
	
	private final Object item;

	public WriteFailureException(String msg, Throwable ex, Object item) {
		super(msg, ex);
		this.item = item;
	}

	public WriteFailureException(String msg) {
		this(msg, null, null);
	}

	public WriteFailureException(Throwable nested) {
		this("", nested, null);
	}
	
	public WriteFailureException(String msg, Object item){
		this(msg, null, item);
	}
	
	public WriteFailureException(Throwable nested, Object item){
		this("", nested, item);
	}
	
	public Object getItem() {
		return item;
	}
}
