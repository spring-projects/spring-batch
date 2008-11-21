package org.springframework.batch.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.batch.item.ItemWriter;

/**
 * Marks a method to be called before an item is passed to an {@link ItemWriter}
 * 
 * @author Lucas Ward
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface BeforeWrite {

}
