package org.springframework.batch.item.file.mapping;

import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Two-phase {@link LineMapper} implementation consisting of tokenization of the
 * line into {@link FieldSet} followed by mapping to item.
 * 
 * @author Robert Kasanicky
 * 
 * @param <T> type of the item
 */
public class DefaultLineMapper<T> implements LineMapper<T>, InitializingBean {

	private LineTokenizer tokenizer;

	private FieldSetMapper<T> fieldSetMapper;

	public T mapLine(String line, int lineNumber) throws Exception {
		return fieldSetMapper.mapFieldSet(tokenizer.tokenize(line));
	}

	public void setLineTokenizer(LineTokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}

	public void setFieldSetMapper(FieldSetMapper<T> fieldSetMapper) {
		this.fieldSetMapper = fieldSetMapper;
	}

	public void afterPropertiesSet() {
		Assert.notNull(tokenizer, "The LineTokenizer must be set");
		Assert.notNull(fieldSetMapper, "The FieldSetMapper must be set");
	}

}
