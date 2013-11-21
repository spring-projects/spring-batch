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
package org.springframework.batch.item.excel.jxl;

import jxl.Workbook;
import jxl.read.biff.WorkbookParser;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.excel.AbstractExcelItemReader;
import org.springframework.batch.item.excel.Sheet;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;

/**
 * {@link ItemReader} implementation which uses the JExcelApi to read an Excel
 * file. It will read the file sheet for sheet and row for row. It is based on
 * the {@link org.springframework.batch.item.file.FlatFileItemReader}
 * 
 * @author Marten Deinum
 * 
 * @param <T> the type
 */
public class JxlItemReader<T> extends AbstractExcelItemReader<T> {

    private Workbook workbook;

    public JxlItemReader() {
        super();
        this.setName(ClassUtils.getShortName(JxlItemReader.class));
    }

    @Override
    protected void openExcelFile(final Resource resource) throws Exception {
        this.workbook = WorkbookParser.getWorkbook(resource.getInputStream());
    }

    @Override
    protected void doCloseWorkbook() throws Exception {
        if (this.workbook != null) {
            this.workbook.close();
        }
    }

    @Override
    protected Sheet getSheet(final int sheet) {
        if (sheet < this.workbook.getNumberOfSheets()) {
            return new JxlSheet(this.workbook.getSheet(sheet));
        }
        return null;
    }

    @Override
    protected int getNumberOfSheets() {
        if (this.workbook == null) {
            throw new IllegalStateException("Workbook file not ready for reading!");
        }
        return this.workbook.getNumberOfSheets();
    }

}
