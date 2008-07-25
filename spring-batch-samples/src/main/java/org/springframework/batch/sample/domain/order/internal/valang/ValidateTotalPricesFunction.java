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
public class ValidateTotalPricesFunction extends AbstractFunction {
    private static final BigDecimal BD_MIN = new BigDecimal(0.0);
    private static final BigDecimal BD_MAX = new BigDecimal(99999999.99);
    private static final BigDecimal BD_100 = new BigDecimal(100.00);

    public ValidateTotalPricesFunction(Function[] arguments, int line, int column) {
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

            if ((BD_MIN.compareTo(item.getTotalPrice()) > 0)
                    || (BD_MAX.compareTo(item.getTotalPrice()) < 0)) {
                return Boolean.FALSE;
            }

            //calculate total price

            //discount coeficient = (100.00 - discountPerc) / 100.00
            BigDecimal coef = BD_100.subtract(item.getDiscountPerc())
                                    .divide(BD_100, 4, BigDecimal.ROUND_HALF_UP);

            //discountedPrice = (price * coef) - discountAmount
            //at least one of discountPerc and discountAmount is 0 - this is validated by ValidateDiscountsFunction 
            BigDecimal discountedPrice = item.getPrice().multiply(coef)
                                             .subtract(item.getDiscountAmount());

            //price for single item = discountedPrice + shipping + handling
            BigDecimal singleItemPrice = discountedPrice.add(item.getShippingPrice())
                                                        .add(item.getHandlingPrice());

            //total price = singleItemPrice * quantity
            BigDecimal quantity = new BigDecimal(item.getQuantity());
            BigDecimal totalPrice = singleItemPrice.multiply(quantity)
                                                   .setScale(2, BigDecimal.ROUND_HALF_UP);

            //calculatedPrice should equal to item.totalPrice  
            if (totalPrice.compareTo(item.getTotalPrice()) != 0) {
                return Boolean.FALSE;
            }
        }

        return Boolean.TRUE;
    }
}
