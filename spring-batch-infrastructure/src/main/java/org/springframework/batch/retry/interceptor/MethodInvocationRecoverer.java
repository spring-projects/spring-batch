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

package org.springframework.batch.retry.interceptor;


/**
 * Strategy interface for recovery action when processing of an item fails.<br/>
 * 
 * @author Dave Syer
 */
public interface MethodInvocationRecoverer<T> {

	/**
	 * Recover gracefully from an error. Clients can call this if processing of
	 * the item throws an unexpected exception. Caller can use the return value
	 * to decide whether to try more corrective action or perhaps throw an
	 * exception.
	 * 
	 * @param args
	 *            the arguments for the method invocation that failed.
	 * @param cause
	 *            the cause of the failure that led to this recovery.
	 * @return the value to be returned to the caller
	 */
	T recover(Object[] args, Throwable cause);
}
