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

package org.springframework.batch.sample.item.provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.io.exception.TransactionInvalidException;
import org.springframework.batch.io.file.FieldSetInputSource;
import org.springframework.batch.io.file.FieldSetMapper;
import org.springframework.batch.item.provider.AbstractItemProvider;

/**
 * @author peter.zozom
 * 
 */
public class SkipSampleItemProvider extends AbstractItemProvider {

	private static Log log = LogFactory.getLog(SkipSampleItemProvider.class);

	private int counter = 0;

	private int exceptionOnrecordNumber = 14;

	private FieldSetInputSource inputSource;

	private FieldSetMapper fieldSetMapper;

	public Object next() {
		counter++;

		if (counter == exceptionOnrecordNumber) {
			// this causes rollback of current transaction
			log.debug("Throwing TransactionInvalidException to cause transaction rollback...");
			throw new TransactionInvalidException("Error processing line: " + counter + ". Rollbacking...");
		}

		return fieldSetMapper.mapLine(inputSource.readFieldSet());
	}

	public void setInputSource(FieldSetInputSource inputTemplate) {
		this.inputSource = inputTemplate;
	}

	public void setFieldSetMapper(FieldSetMapper fieldSetMapper) {
		this.fieldSetMapper = fieldSetMapper;
	}

	public void setThrowExceptionOnRecordNumber(int exceptionOnrecordNumber) {
		this.exceptionOnrecordNumber = exceptionOnrecordNumber;
	}

}
