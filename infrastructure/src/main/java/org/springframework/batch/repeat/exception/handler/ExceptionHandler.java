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

package org.springframework.batch.repeat.exception.handler;

import java.util.Collection;

import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatContext;

/**
 * Policy to allow strategies for rethrowing exceptions in the case of
 * termination. Normally a {@link CompletionPolicy} will be used to decide
 * whether to end a batch including when there is an exception, and the
 * {@link ExceptionHandler} is used to distinguish between normal and abnormal
 * ending. An abnormal ending would normally result in an
 * {@link ExceptionHandler} throwing an exception.
 * 
 * @author Dave Syer
 * 
 */
public interface ExceptionHandler {

	/**
	 * Deal with a collection of throwables accumulated during a batch. The
	 * collection may consist of RuntimeExceptions or other unchecked
	 * exceptions.
	 * @param context the current {@link RepeatContext}. Can be used to store
	 * state (via attributes), for example to count the number of occurrences of
	 * a particular exception type and implement a threshold policy.
	 * @param throwables a collection of non-checked exceptions.
	 * 
	 * @throws any or all of the exception types passed in, or a wrapped
	 * exception if one is an error or checked exception.
	 */
	void handleExceptions(RepeatContext context, Collection throwables) throws RuntimeException;

}
