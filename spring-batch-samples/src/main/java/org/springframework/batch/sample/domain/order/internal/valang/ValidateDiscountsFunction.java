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
public class ValidateDiscountsFunction extends AbstractFunction {
    private static final BigDecimal BD_0 = new BigDecimal(0.0);
    private static final BigDecimal BD_PERC_MAX = new BigDecimal(100.0);

    public ValidateDiscountsFunction(Function[] arguments, int line, int column) {
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

            if (BD_0.compareTo(item.getDiscountPerc()) != 0) {
                //DiscountPerc must be between 0.0 and 100.0
                if ((BD_0.compareTo(item.getDiscountPerc()) > 0)
                        || (BD_PERC_MAX.compareTo(item.getDiscountPerc()) < 0)
                        || (BD_0.compareTo(item.getDiscountAmount()) != 0)) { //only one of DiscountAmount and DiscountPerc should be non-zero

                    return Boolean.FALSE;
                }
            } else {
                //DiscountAmount must be between 0.0 and item.price
                if ((BD_0.compareTo(item.getDiscountAmount()) > 0)
                        || (item.getPrice().compareTo(item.getDiscountAmount()) < 0)) {
                    return Boolean.FALSE;
                }
            }
        }

        return Boolean.TRUE;
    }
}
