/*
 * Copyright 2026-present the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Convenient annotation for injecting a job parameter into a field or a parameter of a
 * bean defined in {@link JobScope &#64;JobScope} or {@link StepScope &#64;StepScope}. The
 * following listing shows an example:
 * </p>
 *
 * <pre class="code">
 * &#064;Bean
 * &#064;StepScope
 * public Callable&lt;String&gt; callable(&#064;JobParameter("key") String value) {
 *     return new SimpleCallable(value);
 * }
 * </pre>
 *
 * <p>
 * Marking a parameter or field as &#64;JobParameter is equivalent to marking it as
 * <code>&#64;Value(&quot;#{jobParameters['key']}&quot;)</code>. As with
 * <code>&#64;Value</code>, a job parameter that is not present in the job parameters is
 * injected as <code>null</code>, and the injection fails on a bean that is neither job
 * scoped nor step scoped, as there is no job or step context to resolve the job parameter
 * from.
 * </p>
 *
 * <p>
 * When no name is specified, the name of the annotated field or parameter is used. For a
 * method or constructor parameter, this requires the code to be compiled with the
 * <code>-parameters</code> flag, otherwise the name has to be specified explicitly.
 * </p>
 *
 * @author Yanming Zhou
 * @author Mahmoud Ben Hassine
 * @since 6.1
 * @see JobScope
 * @see StepScope
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Autowired
public @interface JobParameter {

	/**
	 * The name of the job parameter to inject. Defaults to the name of the annotated
	 * field or parameter.
	 * @return the name of the job parameter
	 */
	String value() default "";

}
