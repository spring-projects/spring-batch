/*
 * Copyright 2012-present the original author or authors.
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
 * Convenient annotation for injecting job parameters in @JobScope or @StepScope. The
 * following listing shows an example:
 * </p>
 *
 * <pre class="code">
 * &#064;Bean
 * &#064;StepScope
 * protected Callable&lt;String&gt; value(@JobParameter final String key) {
 *     return new SimpleCallable(value);
 * }
 * </pre>
 *
 * <p>
 * Marking a parameter or field as &#64;JobParameter is equivalent to marking it as
 * <code>&#64;Value(&quot;#{jobParameters['key']}&quot;)</code>
 * </p>
 *
 * @author Yanming Zhou
 * @since 6.1
 *
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Autowired
public @interface JobParameter {

	String value() default "";

}
