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
package org.springframework.batch.item.file;

/**
 * This interface is used to mark item classes that will be output by the reader 
 * and want to know which line number they come from in the source file. 
 * 
 * @see FlatFileItemReader#doRead() 
 *
 * @author Dean de Bree
 */
public interface LineNumberAwareItem {
    
    /**
     * The method that will be called by the reader to set the line number where 
     * the item was found in the source file
     * 
     * @param lineNumber 
     */
    public void setLineNumber(int lineNumber);
    
}
