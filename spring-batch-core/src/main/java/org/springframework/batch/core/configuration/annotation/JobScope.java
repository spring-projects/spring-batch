/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.configuration.annotation;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>
 * Convenient annotation for job scoped beans that defaults the proxy mode, so that it doesn't have to be specified
 * explicitly on every bean definition. Use this on any &#64;Bean that needs to inject &#64;Values from the job
 * context, and any bean that needs to share a lifecycle with a job execution (e.g. an JobExecutionListener). E.g.
 * </p>
 *
 * <pre class="code">
 * &#064;Bean
 * &#064;JobScope
 * protected Callable&lt;String&gt; value(@Value(&quot;#{jobExecution.jobInstance.jobName}&quot;)
 * final String value) {
 * 	return new SimpleCallable(value);
 * }
 * </pre>
 *
 * <p>Marking a &#64;Bean as &#64;JobScope is equivalent to marking it as <code>&#64;Scope(value="job", proxyMode=TARGET_CLASS)</code></p>
 *
 * @author Michael Minella
 *
 * @since 3.0.1
 *
 */
@Scope(value = "job", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JobScope {

}