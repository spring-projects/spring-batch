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

import java.util.Date;

import org.springmodules.validation.valang.functions.AbstractFunction;
import org.springmodules.validation.valang.functions.Function;


/**
 * Returns Boolean.TRUE if given value is future date, else it returns Boolean.FALSE
 * @author peter.zozom
 */
public class FutureDateFunction extends AbstractFunction {

    public FutureDateFunction(Function[] arguments, int line, int column) {
        super(arguments, line, column);
        definedExactNumberOfArguments(1);
    }

    /**
     * @see org.springmodules.validation.valang.functions.AbstractFunction#doGetResult(java.lang.Object)
     */
    protected Object doGetResult(final Object target) throws Exception {
        //get argument
        final Object value = getArguments()[0].getResult(target);

        Boolean result;

        if (value instanceof Date) {
            final Date now = new Date(System.currentTimeMillis());
            final Date date = (Date) value;
            result = (now.compareTo(date) < 0) ? Boolean.TRUE : Boolean.FALSE;
        } else {
            throw new Exception("No Date value for validation");
        }

        return result;
    }
}
