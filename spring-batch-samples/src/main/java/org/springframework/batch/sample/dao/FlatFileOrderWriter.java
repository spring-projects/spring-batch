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

import org.springframework.batch.item.transform.ItemTransformer;
import org.springframework.batch.item.writer.DelegatingItemWriter;
import org.springframework.batch.sample.item.writer.OrderWriter;


/**
 * Writes <code>Order</code> objects to a file.
 * 
 * @see OrderWriter
 * 
 * @author Dave Syer
 */
public class FlatFileOrderWriter extends DelegatingItemWriter {

    /**
     * Converter for order
     */
    private ItemTransformer transformer = new OrderTransformer();
    
    /**
	 * Public setter for the converter.
	 *
	 * @param transformer the converter to set
	 */
	public void setTransformer(ItemTransformer transformer) {
		this.transformer = transformer;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.writer.DelegatingItemWriter#doProcess(java.lang.Object)
	 */
	protected Object doProcess(Object item) throws Exception {
        return transformer.transform(item);
    }

}
