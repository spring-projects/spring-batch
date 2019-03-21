/*
 * Copyright 2006-2013 the original author or authors.
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
package org.springframework.batch.core.scope;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.scope.context.JobContext;
import org.springframework.batch.core.scope.context.JobSynchronizationManager;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

/**
 * Scope for job context. Objects in this scope use the Spring container as an
 * object factory, so there is only one instance of such a bean per executing
 * job. All objects in this scope are &lt;aop:scoped-proxy/&gt; (no need to
 * decorate the bean definitions).<br>
 * <br>
 *
 * In addition, support is provided for late binding of references accessible
 * from the {@link JobContext} using #{..} placeholders. Using this feature,
 * bean properties can be pulled from the job or job execution context and the
 * job parameters. E.g.
 *
 * <pre>
 * &lt;bean id=&quot;...&quot; class=&quot;...&quot; scope=&quot;job&quot;&gt;
 * 	&lt;property name=&quot;name&quot; value=&quot;#{jobParameters[input]}&quot; /&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id=&quot;...&quot; class=&quot;...&quot; scope=&quot;job&quot;&gt;
 * 	&lt;property name=&quot;name&quot; value=&quot;#{jobExecutionContext['input.stem']}.txt&quot; /&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * The {@link JobContext} is referenced using standard bean property paths (as
 * per {@link BeanWrapper}). The examples above all show the use of the Map
 * accessors provided as a convenience for job attributes.
 *
 * @author Dave Syer
 * @author Jimmy Praet (create JobScope based on {@link StepScope})
 * @author Michael Minella
 * @since 3.0
 */
public class JobScope extends BatchScopeSupport {

	private static final String TARGET_NAME_PREFIX = "jobScopedTarget.";

	private Log logger = LogFactory.getLog(getClass());

	private final Object mutex = new Object();

	/**
	 * Context key for clients to use for conversation identifier.
	 */
	public static final String ID_KEY = "JOB_IDENTIFIER";

	public JobScope() {
		super();
		setName("job");
	}

	/**
	 * This will be used to resolve expressions in job-scoped beans.
	 */
	@Override
	public Object resolveContextualObject(String key) {
		JobContext context = getContext();
		// TODO: support for attributes as well maybe (setters not exposed yet
		// so not urgent).
		return new BeanWrapperImpl(context).getPropertyValue(key);
	}

	/**
	 * @see Scope#get(String, ObjectFactory)
	 */
	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		JobContext context = getContext();
		Object scopedObject = context.getAttribute(name);

		if (scopedObject == null) {

			synchronized (mutex) {
				scopedObject = context.getAttribute(name);
				if (scopedObject == null) {

					if (logger.isDebugEnabled()) {
						logger.debug(String.format("Creating object in scope=%s, name=%s", this.getName(), name));
					}

					scopedObject = objectFactory.getObject();
					context.setAttribute(name, scopedObject);

				}

			}

		}
		return scopedObject;
	}

	/**
	 * @see Scope#getConversationId()
	 */
	@Override
	public String getConversationId() {
		JobContext context = getContext();
		return context.getId();
	}

	/**
	 * @see Scope#registerDestructionCallback(String, Runnable)
	 */
	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		JobContext context = getContext();
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Registered destruction callback in scope=%s, name=%s", this.getName(), name));
		}
		context.registerDestructionCallback(name, callback);
	}

	/**
	 * @see Scope#remove(String)
	 */
	@Override
	public Object remove(String name) {
		JobContext context = getContext();
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Removing from scope=%s, name=%s", this.getName(), name));
		}
		return context.removeAttribute(name);
	}

	/**
	 * Get an attribute accessor in the form of a {@link JobContext} that can
	 * be used to store scoped bean instances.
	 *
	 * @return the current job context which we can use as a scope storage
	 *         medium
	 */
	private JobContext getContext() {
		JobContext context = JobSynchronizationManager.getContext();
		if (context == null) {
			throw new IllegalStateException("No context holder available for job scope");
		}
		return context;
	}

	@Override
	public String getTargetNamePrefix() {
		return TARGET_NAME_PREFIX;
	}
}
