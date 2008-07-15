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

package org.springframework.batch.repeat.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.repeat.RepeatContext;

public class RepeatContextSupport extends SynchronizedAttributeAccessor implements RepeatContext {

	private RepeatContext parent;

	private int count;

	private volatile boolean completeOnly;

	private volatile boolean terminateOnly;

	private Map<String, Set<Runnable>> callbacks = new HashMap<String, Set<Runnable>>();

	/**
	 * Constructor for {@link RepeatContextSupport}. The parent can be null, but
	 * should be set to the enclosing repeat context if there is one, e.g. if
	 * this context is an inner loop.
	 * @param parent
	 */
	public RepeatContextSupport(RepeatContext parent) {
		super();
		this.parent = parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.repeat.RepeatContext#isCompleteOnly()
	 */
	public boolean isCompleteOnly() {
		return completeOnly;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.repeat.RepeatContext#setCompleteOnly()
	 */
	public void setCompleteOnly() {
		completeOnly = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.repeat.RepeatContext#isTerminateOnly()
	 */
	public boolean isTerminateOnly() {
		return terminateOnly;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.repeat.RepeatContext#setTerminateOnly()
	 */
	public void setTerminateOnly() {
		terminateOnly = true;
		setCompleteOnly();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.repeat.RepeatContext#getParent()
	 */
	public RepeatContext getParent() {
		return parent;
	}

	/**
	 * Used by clients to increment the started count.
	 */
	public synchronized void increment() {
		count++;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.repeat.RepeatContext#getStartedCount()
	 */
	public synchronized int getStartedCount() {
		return count;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.repeat.RepeatContext#registerDestructionCallback
	 * (java.lang.String, java.lang.Runnable)
	 */
	public void registerDestructionCallback(String name, Runnable callback) {
		synchronized (callbacks) {
			Set<Runnable> set = callbacks.get(name);
			if (set == null) {
				set = new HashSet<Runnable>();
				callbacks.put(name, set);
			}
			set.add(callback);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.repeat.RepeatContext#close()
	 */
	public void close() {

		List<RuntimeException> errors = new ArrayList<RuntimeException>();

		Set<Map.Entry<String, Set<Runnable>>> copy;

		synchronized (callbacks) {
			copy = new HashSet<Map.Entry<String, Set<Runnable>>>(callbacks.entrySet());
		}

		for (Map.Entry<String, Set<Runnable>> entry : copy) {

			for (Runnable callback : entry.getValue()) {
				/*
				 * Potentially we could check here if there is an attribute with
				 * the given name - if it has been removed, maybe the callback
				 * is invalid. On the other hand it is less surprising for the
				 * callback register if it is always executed.
				 */
				if (callback != null) {
					/*
					 * The documentation of the interface says that these
					 * callbacks must not throw exceptions, but we don't trust
					 * them necessarily...
					 */
					try {
						callback.run();
					}
					catch (RuntimeException t) {
						errors.add(t);
					}
				}
			}
		}

		if (errors.isEmpty()) {
			return;
		}

		throw (RuntimeException) errors.get(0);
	}

}
