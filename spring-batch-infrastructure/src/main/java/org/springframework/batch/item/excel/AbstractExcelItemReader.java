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
package org.springframework.batch.item.excel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link ItemReader} implementation which uses the JExcelApi to read an Excel
 * file. It will read the file sheet for sheet and row for row. It is based on
 * the {@link org.springframework.batch.item.file.FlatFileItemReader}
 *
 * @param <T> the type
 * @author Marten Deinum
 */
public abstract class AbstractExcelItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> implements
        ResourceAwareItemReaderItemStream<T>, InitializingBean {

    protected final Log logger = LogFactory.getLog(getClass());
    private Resource resource;
    private int linesToSkip = 0;
    private int currentRow = 0;
    private int currentSheet = 0;
    private RowMapper<T> rowMapper;
    private RowCallbackHandler skippedRowsCallback;
    private boolean noInput = false;
    private boolean strict = true;

    public AbstractExcelItemReader() {
        super();
        this.setName(ClassUtils.getShortName(this.getClass()));
    }

    @Override
    protected T doRead() throws Exception {
        if (this.noInput) {
            return null;
        }
        final Sheet sheet = this.getSheet(this.currentSheet);
        final String[] row = this.readRow(sheet);
        if (ObjectUtils.isEmpty(row)) {
            this.currentSheet++;
            if (this.currentSheet >= this.getNumberOfSheets()) {
                if (logger.isDebugEnabled() ) {
                    logger.debug("No more sheets in '" + this.resource.getDescription() + "'.");
                }
                return null;
            } else {
                this.currentRow = 0;
                this.openSheet();
                return this.doRead();
            }
        } else {
            try {
                return this.rowMapper.mapRow(sheet, row, this.currentRow);
            } catch (final Exception e) {
                throw new ExcelFileParseException("Exception parsing Excel file.", e, this.resource.getDescription(),
                        sheet.getName(), this.currentRow, row);
            }
        }
    }

    @Override
    protected void doOpen() throws Exception {
        Assert.notNull(this.resource, "Input resource must be set");
        this.noInput = true;
        if (!this.resource.exists()) {
            if (this.strict) {
                throw new IllegalStateException("Input resource must exist (reader is in 'strict' mode): "
                        + this.resource);
            }
            logger.warn("Input resource does not exist '"+this.resource.getDescription()+"'.");
            return;
        }

        if (!this.resource.isReadable()) {
            if (this.strict) {
                throw new IllegalStateException("Input resource must be readable (reader is in 'strict' mode): "
                        + this.resource);
            }
            logger.warn("Input resource is not readable '"+this.resource.getDescription()+"'.");
            return;
        }

        this.noInput = false;
        this.openExcelFile(this.resource);
        this.openSheet();
        if (logger.isDebugEnabled()) {
            logger.debug("Opened workbook ["+this.resource.getFilename()+"] with "+this.getNumberOfSheets()+" sheets.");
        }
    }

    private String[] readRow(final Sheet sheet) {
        this.currentRow++;
        if (this.currentRow < sheet.getNumberOfRows()) {
            return sheet.getRow(this.currentRow);
        }
        return null;
    }

    private void openSheet() {
        final Sheet sheet = this.getSheet(this.currentSheet);
        if (logger.isDebugEnabled()) {
            logger.debug("Opening sheet "+sheet.getName()+".");
        }
        for (int i = 0; i < this.linesToSkip; i++) {
            final String[] row = this.readRow(sheet);
            if (this.skippedRowsCallback != null) {
                this.skippedRowsCallback.handleRow(sheet, row);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Openend sheet "+sheet.getName()+", with "+sheet.getNumberOfRows()+" rows.");
        }

    }

    @Override
    protected final void doClose() throws Exception {
        doCloseWorkbook();
        if (getResource() != null) {
            try {
                InputStream is = getResource().getInputStream();
                is.close();
            } catch (IOException ioe) {
                logger.warn("Exception whilst obtaining or closing the inputstream.", ioe);
            }
        }
    }

    /**
     * Method which can be overriden by subclasses to do cleanup additional resources.
     *
     * @throws Exception
     */
    protected void doCloseWorkbook() throws Exception {
    }

    protected Resource getResource() {
        return this.resource;
    }

    public void setResource(final Resource resource) {
        this.resource = resource;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.rowMapper, "RowMapper must be set");
    }

    /**
     * Set the number of lines to skip. This number is applied to all worksheet
     * in the excel file! default to 0
     *
     * @param linesToSkip
     */
    public void setLinesToSkip(final int linesToSkip) {
        this.linesToSkip = linesToSkip;
    }

    protected abstract Sheet getSheet(int sheet);

    protected abstract int getNumberOfSheets();

    protected abstract void openExcelFile(Resource resource) throws Exception;

    /**
     * In strict mode the reader will throw an exception on
     * {@link #open(org.springframework.batch.item.ExecutionContext)} if the input resource does not exist.
     *
     * @param strict true by default
     */
    public void setStrict(final boolean strict) {
        this.strict = strict;
    }

    public void setRowMapper(final RowMapper<T> rowMapper) {
        this.rowMapper = rowMapper;
    }

    public void setSkippedRowsCallback(final RowCallbackHandler skippedRowsCallback) {
        this.skippedRowsCallback = skippedRowsCallback;
    }
}
