/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.sample.domain.trade.internal.validator;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.batch.sample.domain.trade.Trade;

/**
 * @author Michael Minella
 */
public class TradeValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.equals(Trade.class);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Trade trade = (Trade) target;

        if(trade.getIsin().length() >= 13) {
            errors.rejectValue("isin", "isin_length");
        }
    }
}
