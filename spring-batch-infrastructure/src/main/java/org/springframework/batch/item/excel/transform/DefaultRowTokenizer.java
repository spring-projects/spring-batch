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
 */package org.springframework.batch.item.excel.transform;

import org.springframework.batch.item.excel.Sheet;
import org.springframework.batch.item.file.transform.DefaultFieldSetFactory;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.FieldSetFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link RowTokenizer} which assumes the column names are on the first row in the sheet. 
 * 
 * @author Marten Deinum
 */
public class DefaultRowTokenizer implements RowTokenizer, InitializingBean {

    private FieldSetFactory fieldSetFactory = new DefaultFieldSetFactory();

    private ColumnToAttributeConverter converter = new PassThroughColumnToAttributeConverter();

    private boolean useColumnHeader = true;

    private boolean includeSheetName = false;
    private String attributeForSheetName = null;

    public FieldSet tokenize(final Sheet sheet, final String[] row) {
        String[] values = new String[sheet.getNumberOfColumns()];
        System.arraycopy(row, 0, values, 0, row.length);

        if (this.includeSheetName) {
            values = StringUtils.addStringToArray(values, sheet.getName());
        }

        if (this.useColumnHeader) {
            String[] names = sheet.getHeader();
            if (this.includeSheetName) {
                names = StringUtils.addStringToArray(names, this.attributeForSheetName);
            }
            for (int i = 0; i < names.length; i++) {
                names[i] = this.converter.toAttribute(names[i]);
            }

            return this.fieldSetFactory.create(values, names);
        } else {
            return this.fieldSetFactory.create(values);
        }

    }

    /**
     * Set the {@link FieldSetFactory} to use. The {@link DefaultFieldSetFactory} is used by default.
     * 
     * @param fieldSetFactory to set
     */
    public void setFieldSetFactory(final FieldSetFactory fieldSetFactory) {
        this.fieldSetFactory = fieldSetFactory;
    }

    /**
     * Indication to use the column header, the default is <code>true</code>.
     * @param useColumnHeader
     */
    public void setUseColumnHeader(final boolean useColumnHeader) {
        this.useColumnHeader = useColumnHeader;
    }

    /**
     * Set the {@link ColumnToAttributeConverter}.
     * 
     * @param converter to set
     */
    public void setConverter(final ColumnToAttributeConverter converter) {
        this.converter = converter;
    }

    public void setIncludeSheetName(final boolean includeSheetName) {
        this.includeSheetName = includeSheetName;
    }

    public void setAttributeForSheetName(final String attributeForSheetName) {
        this.attributeForSheetName = attributeForSheetName;
    }

    public void afterPropertiesSet() throws Exception {
        if (this.includeSheetName && this.useColumnHeader) {
            Assert.hasText(this.attributeForSheetName,
                    "When using column header as attributes and including the sheetname an attribute name for the sheetname is required.");
        }
    }
}
