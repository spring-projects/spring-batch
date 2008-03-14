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

package org.springframework.batch.core.repository.dao;

/**
 * This exception identifies that a batch domain object is invalid, which
 * is generally caused by an invalid ID. (An ID which doesn't exist in the database).
 * 
 * @author Lucas Ward
 * @author Dave Syer
 *
 */
public class NoSuchObjectException extends RuntimeException {

	private static final long serialVersionUID = 4399621765157283111L;

	public NoSuchObjectException(String message){
		super(message);
	}
}
