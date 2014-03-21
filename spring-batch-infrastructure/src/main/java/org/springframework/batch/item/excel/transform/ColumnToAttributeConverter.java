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

/**
 * Convert a column name to an attribute name and vice versa.
 * 
 * @author Marten Deinum
 */
public interface ColumnToAttributeConverter {

    /**
     * Convert a column name to an attribute name.
     * 
     * @param column to convert
     * @return the attribute name
     */
    String toAttribute(String column);

    /**
     * Convert an attribute name to a column name.
     * 
     * @param attribute to convert
     * @return the column name
     */
    String toColumn(String attribute);

}
