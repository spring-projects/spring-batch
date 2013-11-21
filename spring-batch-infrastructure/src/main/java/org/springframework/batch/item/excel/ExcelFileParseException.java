/*
 * Copyright 2007-2011 the original author or authors.
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

import org.springframework.batch.item.ParseException;

/**
 * Exception thrown when parsing excel files. The name of the sheet, the row number on that sheet and the
 * name of the excel file can be passed in so that in exception handling we can reuse it. This class only has 
 * simply dependencies to make it is generic as possible.
 * 
 * @author Marten Deinum
 *
 */
public class ExcelFileParseException extends ParseException {

    /**
     * 
     */
    private static final long serialVersionUID = -3939056060545496492L;

    private final String filename;
    private final String sheet;
    private final String[] row;
    private final int rowNumber;

    /**
     * Construct an {@link ExcelFileParseException}.
     * 
     * @param message the message
     * @param cause the root cause
     * @param filename the name of the excel file
     * @param sheet the name of the sheet
     * @param rowNumber the row number in the current sheet
     * @param row the row data as text
     */
    public ExcelFileParseException(final String message, final Throwable cause, final String filename,
            final String sheet, final int rowNumber, final String[] row) {
        super(message, cause);
        this.filename = filename;
        this.sheet = sheet;
        this.rowNumber = rowNumber;
        this.row = row;
    }

    public String getFilename() {
        return this.filename;
    }

    public String getSheet() {
        return this.sheet;
    }

    public int getRowNumber() {
        return this.rowNumber;
    }

    public String[] getRow() {
        return this.row;
    }

}
