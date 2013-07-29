package org.springframework.batch.core.configuration.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * <p>
 * Convenient annotation for step scoped beans that defaults the proxy mode, so that it doesn't have to be specified
 * explicitly on every bean definition. Use this on any &#64;Bean that needs to inject &#64;Values from the step
 * context, and any bean that needs to share a lifecycle with a step execution (e.g. an ItemStream). E.g.
 * </p>
 *
 * <pre class="code">
 * &#064;Bean
 * &#064;StepScope
 * protected Callable&lt;String&gt; value(@Value(&quot;#{stepExecution.stepName}&quot;)
 * final String value) {
 * 	return new SimpleCallable(value);
 * }
 * </pre>
 *
 * <p>Marking a &#64;Bean as &#64;StepScope is equivalent to marking it as <code>&#64;Scope(value="step", proxyMode=TARGET_CLASS)</code></p>
 *
 * @author Dave Syer
 *
 * @Since 2.2
 *
 */
@Scope(value = "step", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface StepScope {

}