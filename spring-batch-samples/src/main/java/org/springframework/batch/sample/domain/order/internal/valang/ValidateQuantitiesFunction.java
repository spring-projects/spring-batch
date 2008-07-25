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

package org.springframework.batch.sample.domain.order.internal.valang;

import java.util.List;

import org.springframework.batch.sample.domain.order.LineItem;
import org.springmodules.validation.valang.functions.AbstractFunction;
import org.springmodules.validation.valang.functions.Function;


/**
 * @author peter.zozom
 *
 */
public class ValidateQuantitiesFunction extends AbstractFunction {
    private static final int MAX_QUANTITY = 9999;

    public ValidateQuantitiesFunction(Function[] arguments, int line, int column) {
        super(arguments, line, column);
        definedExactNumberOfArguments(1);
    }

    /**
     * @see org.springmodules.validation.valang.functions.AbstractFunction#doGetResult(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
	protected Object doGetResult(Object target) throws Exception {
    	 List<LineItem> lineItems = (List<LineItem>) getArguments()[0].getResult(target);

         for (LineItem item : lineItems) {

            if ((item.getQuantity() <= 0) || (item.getQuantity() > MAX_QUANTITY)) {
                return Boolean.FALSE;
            }
        }

        return Boolean.TRUE;
    }
}
