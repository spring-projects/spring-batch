/*
 * Copyright 2009-2012 the original author or authors.
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

/**
 * Default implementation of {@link FieldSetFactory} with no special knowledge
 * of the {@link FieldSet} required. Returns a {@link DefaultFieldSet} from both
 * factory methods.
 * 
 * @author Dave Syer
 * 
 */
public class DefaultFieldSetFactory implements FieldSetFactory {

	private DateFormat dateFormat;

	private NumberFormat numberFormat;

	/**
	 * The {@link NumberFormat} to use for parsing numbers. If unset the default
	 * locale will be used.
	 * @param numberFormat the {@link NumberFormat} to use for number parsing
	 */
	public void setNumberFormat(NumberFormat numberFormat) {
		this.numberFormat = numberFormat;
	}

	/**
	 * The {@link DateFormat} to use for parsing numbers. If unset the default
	 * pattern is ISO standard <code>yyyy/MM/dd</code>.
	 * @param dateFormat the {@link DateFormat} to use for date parsing
	 */
	public void setDateFormat(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}

	/**
	 * {@inheritDoc}
	 */
    @Override
	public FieldSet create(String[] values, String[] names) {
		DefaultFieldSet fieldSet = new DefaultFieldSet(values, names);
		return enhance(fieldSet);
	}

	/**
	 * {@inheritDoc}
	 */
    @Override
	public FieldSet create(String[] values) {
		DefaultFieldSet fieldSet = new DefaultFieldSet(values);
		return enhance(fieldSet);
	}

	private FieldSet enhance(DefaultFieldSet fieldSet) {
		if (dateFormat!=null) {
			fieldSet.setDateFormat(dateFormat);
		}
		if (numberFormat!=null) {
			fieldSet.setNumberFormat(numberFormat);
		}	
		return fieldSet;
	}

}
