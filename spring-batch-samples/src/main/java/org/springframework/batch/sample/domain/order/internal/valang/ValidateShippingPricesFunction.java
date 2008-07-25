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

import java.math.BigDecimal;
import java.util.List;

import org.springframework.batch.sample.domain.order.LineItem;
import org.springmodules.validation.valang.functions.AbstractFunction;
import org.springmodules.validation.valang.functions.Function;


/**
 * @author peter.zozom
 *
 */
public class ValidateShippingPricesFunction extends AbstractFunction {
    private static final BigDecimal BD_MIN = new BigDecimal(0.0);
    private static final BigDecimal BD_MAX = new BigDecimal(99999999.99);

    public ValidateShippingPricesFunction(Function[] arguments, int line, int column) {
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

            if ((BD_MIN.compareTo(item.getShippingPrice()) > 0)
                    || (BD_MAX.compareTo(item.getShippingPrice()) < 0)) {
                return Boolean.FALSE;
            }
        }

        return Boolean.TRUE;
    }
}
