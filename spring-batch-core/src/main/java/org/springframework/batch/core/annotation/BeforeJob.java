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

package org.springframework.batch.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Marks a method to be called before a {@link Job} is executed, which comes
 * after a {@link JobExecution} is created and persisted, but before the first
 * {@link Step} is executed. <br>
 * <br>
 * Expected signature: void beforeJob({@link JobExecution} jobExecution)
 * 
 * @author Lucas Ward
 * @since 2.0
 * @see JobExecutionListener
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Qualifier("JobExecutionListener")
public @interface BeforeJob {

}
