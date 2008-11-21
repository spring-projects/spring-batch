package org.springframework.batch.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.batch.item.ItemProcessor;

/**
 * Marks a method to be called when an item is skipped due to an exception thrown in the
 * {@link ItemProcessor}
 * 
 * @author Lucas Ward
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface OnSkipInProcess {

}
