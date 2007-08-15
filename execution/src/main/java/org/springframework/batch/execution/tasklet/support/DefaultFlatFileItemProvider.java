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

package org.springframework.batch.execution.tasklet.support;

import java.util.Properties;

import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.FieldSetMapper;
import org.springframework.batch.item.provider.AbstractFieldSetItemProvider;
import org.springframework.batch.item.validator.Validator;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.batch.statistics.StatisticsProvider;

/**
 * 
 * Uses a {@link FieldSetMapper} to convert each line from an input source. Also
 * adds {@link Restartable} as mandatory behaviour, delegating to the parent
 * provider's input source.
 * 
 * @author Dave Syer
 */
public class DefaultFlatFileItemProvider extends AbstractFieldSetItemProvider implements Restartable,
		StatisticsProvider {

	private FieldSetMapper mapper;
	private Validator validator;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.provider.AbstractFieldSetItemProvider#doNext(org.springframework.batch.io.line.FieldSet)
	 */
	protected Object transform(FieldSet fieldSet) {
		Object value = mapper.mapLine(fieldSet);
		if (validator!=null) {
			validator.validate(value);
		}
		return value;
	}

	/**
	 * @param mapper the mapper to set
	 */
	public void setMapper(FieldSetMapper mapper) {
		this.mapper = mapper;
	}
	
	/**
	 * @param validator the validator to set
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	/**
	 * @see Restartable#getRestartData()
	 * @throws IllegalStateException if the parent template is not itself
	 * {@link Restartable}.
	 */
	public RestartData getRestartData() {
		if (!(source instanceof Restartable)) {
			throw new IllegalStateException("Input Template is not Restartable");
		}
		return ((Restartable) source).getRestartData();
	}

	/**
	 * @see Restartable#restoreFrom(RestartData)
	 * @throws IllegalStateException if the parent template is not itself
	 * {@link Restartable}.
	 */
	public void restoreFrom(RestartData data) {
		if (!(source instanceof Restartable)) {
			throw new IllegalStateException("Input Template is not Restartable");
		}
		((Restartable) source).restoreFrom(data);
	}

	/**
	 * @return delegates to the parent template of it is a
	 * {@link StatisticsProvider}, otherwise returns an empty
	 * {@link Properties} instance.
	 * @see StatisticsProvider#getStatistics()
	 */
	public Properties getStatistics() {
		if (!(source instanceof StatisticsProvider)) {
			return new Properties();
		}
		return ((StatisticsProvider) source).getStatistics();
	}

}
