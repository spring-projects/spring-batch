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
package org.springframework.batch.execution.scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.repeat.context.SynchronizedAttributeAccessor;
import org.springframework.batch.statistics.StatisticsProvider;
import org.springframework.batch.statistics.StatisticsService;

/**
 * Simple implementation of {@link StepContext}.
 * 
 * @author Dave Syer
 * 
 */
public class SimpleStepContext extends SynchronizedAttributeAccessor implements
		StepContext, StatisticsProvider {

	private Map callbacks = new HashMap();
	private StepContext parent;
	private StepExecution stepExecution;
	private StatisticsService statisticsService;

	/**
	 * Default constructor.
	 */
	public SimpleStepContext(StepExecution stepExecution) {
		this(stepExecution, null, null);
	}

	/**
	 * Default constructor.
	 */
	public SimpleStepContext(StepExecution stepExecution, StepContext parent) {
		this(stepExecution, parent, null);
	}

	/**
	 * @param object
	 */
	public SimpleStepContext(StepExecution stepExecution, StepContext parent, StatisticsService statisticsService) {
		super();
		this.parent = parent;
		this.statisticsService = statisticsService;
		this.stepExecution = stepExecution;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.repeat.context.SynchronizedAttributeAccessor#setAttribute(java.lang.String, java.lang.Object)
	 */
	public void setAttribute(String name, Object value) {
		super.setAttribute(name, value);
		if (statisticsService!=null && (value instanceof StatisticsProvider)) {
			statisticsService.register(this, (StatisticsProvider) value);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.statistics.StatisticsProvider#getStatistics()
	 */
	public Properties getStatistics() {
		if (statisticsService==null) {
			return new Properties();
		}
		return statisticsService.getStatistics(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.execution.scope.StepContext#getParent()
	 */
	public StepContext getParent() {
		return parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.repeat.RepeatContext#registerDestructionCallback(java.lang.String,
	 *      java.lang.Runnable)
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.execution.scope.StepContext#registerDestructionCallback(java.lang.String,
	 *      java.lang.Runnable)
	 */
	public void registerDestructionCallback(String name, Runnable callback) {
		synchronized (callbacks) {
			Set set = (Set) callbacks.get(name);
			if (set == null) {
				set = new HashSet();
				callbacks.put(name, set);
			}
			set.add(callback);
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.execution.scope.StepContext#close()
	 */
	public void close() {

		List errors = new ArrayList();

		Set copy;

		synchronized (callbacks) {
			copy = new HashSet(callbacks.entrySet());
		}

		for (Iterator iter = copy.iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			Set set = (Set) entry.getValue();
			for (Iterator iterator = set.iterator(); iterator.hasNext();) {
				Runnable callback = (Runnable) iterator.next();
				/*
				 * There used to be a check here to make sure there was an
				 * attribute with the given name, but an inner bean is not
				 * registered with the bean factory, so the destroy method is
				 * only called in inner bean if we make the callback
				 * unconditionally.
				 */
				if (callback != null) {
					/*
					 * The documentation of the interface says that these
					 * callbacks must not throw exceptions, but we don't trust
					 * them necessarily...
					 */
					try {
						callback.run();
					} catch (RuntimeException t) {
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.execution.scope.StepContext#getJobIdentifier()
	 */
	public StepExecution getStepExecution() {
		return stepExecution;
	}

}
