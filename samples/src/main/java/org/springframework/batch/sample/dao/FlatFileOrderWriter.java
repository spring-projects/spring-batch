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

package org.springframework.batch.sample.dao;

import org.springframework.batch.io.OutputSource;
import org.springframework.batch.io.file.support.transform.Converter;
import org.springframework.batch.sample.domain.Order;


/**
 * Writes <code>Order</code> objects to a file.
 * 
 * @see OrderWriter
 * 
 * @author Dave Syer
 */
public class FlatFileOrderWriter implements OrderWriter {
    /**
     * Takes care of writing to a file
     */
    private OutputSource outputSource;

    /**
     * Converter for order
     */
    private Converter converter = new OrderConverter();
    
    /**
	 * Public setter for the converter.
	 *
	 * @param converter the converter to set
	 */
	public void setConverter(Converter converter) {
		this.converter = converter;
	}

    /**
     * Writes information from an Order object to a file
     */
    public void write(Order data) {
        outputSource.write(converter.convert(data));
    }
    
	public void setOutputSource(OutputSource outputSource) {
        this.outputSource = outputSource;
    }

}
