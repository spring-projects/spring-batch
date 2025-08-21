/*
 * Copyright 2009-2025 the original author or authors.
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
package org.springframework.batch.item.file.transform;

import java.text.DateFormat;
import java.text.NumberFormat;

import org.jspecify.annotations.Nullable;

/**
 * Default implementation of {@link FieldSetFactory} with no special knowledge of the
 * {@link FieldSet} required. Returns a {@link DefaultFieldSet} from both factory methods.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 */
public class DefaultFieldSetFactory implements FieldSetFactory {

	private @Nullable DateFormat dateFormat;

	private @Nullable NumberFormat numberFormat;

	/**
	 * Default constructor.
	 */
	public DefaultFieldSetFactory() {
	}

	/**
	 * Convenience constructor
	 * @param dateFormat the {@link DateFormat} to use for parsing dates
	 * @param numberFormat the {@link NumberFormat} to use for parsing numbers
	 * @since 5.2
	 */
	public DefaultFieldSetFactory(DateFormat dateFormat, NumberFormat numberFormat) {
		this.dateFormat = dateFormat;
		this.numberFormat = numberFormat;
	}

	/**
	 * The {@link DateFormat} to use for parsing dates.
	 * <p>
	 * If unset the default pattern is ISO standard <code>yyyy-MM-dd</code>.
	 * @param dateFormat the {@link DateFormat} to use for date parsing
	 */
	public void setDateFormat(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}

	/**
	 * The {@link NumberFormat} to use for parsing numbers.
	 * <p>
	 * If unset, {@link java.util.Locale#US} will be used.
	 * @param numberFormat the {@link NumberFormat} to use for number parsing
	 */
	public void setNumberFormat(NumberFormat numberFormat) {
		this.numberFormat = numberFormat;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FieldSet create(String[] values, String[] names) {
		return new DefaultFieldSet(values, names, dateFormat, numberFormat);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public FieldSet create(String[] values) {
		return new DefaultFieldSet(values, dateFormat, numberFormat);
	}

}
