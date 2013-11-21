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

/**
 * Interface to wrap different Excel implementations like JExcel, JXL or Apache POI. 
 * 
 * @author Marten Deinum
 *
 */
public interface Sheet {

    /**
     * Get the number of rows in this sheet.
     * 
     * @return the number of rows.
     */
    int getNumberOfRows();

    /**
     * Get the name of the sheet.
     * 
     * @return the name of the sheet.
     */
    String getName();

    /**
     * Get the row as a String[]. Returns null if the row doesn't exist.
     * 
     * @param rowNumber the row number to read.
     * @return a String[] or null
     */
    String[] getRow(int rowNumber);

    /**
     * Gets the first row of the sheet and use it as header.
     * 
     * @return String[] of row 0
     */
    String[] getHeader();

    /**
     * The number of columns in this sheet.
     * 
     * @return number of columns
     */
    int getNumberOfColumns();
}
