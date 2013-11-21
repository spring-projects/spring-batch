/*
 * Copyright 2011 the original author or authors.
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
package org.springframework.batch.item.excel.mapping;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import org.springframework.batch.item.excel.RowMapper;
import org.springframework.batch.item.excel.Sheet;
import org.springframework.batch.item.excel.transform.DefaultRowTokenizer;
import org.springframework.batch.item.excel.transform.RowTokenizer;

/**
 * {@link RowMapper} implementation which delegates to a {@link RowTokenizer} and a {@link FieldSetMapper} the mapping
 * of fields and construction of objects.
 *
 * @author Marten Deinum
 *
 * @param <T>
 */
public class DefaultRowMapper<T> implements RowMapper<T>, InitializingBean {

    private RowTokenizer rowTokenizer = new DefaultRowTokenizer();
    private FieldSetMapper<T> fieldSetMapper;

    public T mapRow(final Sheet sheet, final String[] row, final int rowNum) throws Exception {
        return this.fieldSetMapper.mapFieldSet(this.rowTokenizer.tokenize(sheet, row));
    }

    public void setFieldSetMapper(final FieldSetMapper<T> fieldSetMapper) {
        this.fieldSetMapper = fieldSetMapper;
    }

    /**
     * Set the {@link RowTokenizer} to use to create a {@link FieldSet}. Default uses the {@link DefaultRowTokenizer}.
     * 
     * @param rowTokenizer to use
     */
    public void setRowTokenizer(final RowTokenizer rowTokenizer) {
        this.rowTokenizer = rowTokenizer;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.rowTokenizer, "The RowTokenizer must be set");
        Assert.notNull(this.fieldSetMapper, "The FieldSetMapper must be set");
    }
}
