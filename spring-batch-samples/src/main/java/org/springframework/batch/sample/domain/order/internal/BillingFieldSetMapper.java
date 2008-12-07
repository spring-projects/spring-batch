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

package org.springframework.batch.sample.domain.order.internal;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.sample.domain.order.BillingInfo;



public class BillingFieldSetMapper implements FieldSetMapper<BillingInfo> {
	
	public static final String PAYMENT_TYPE_ID_COLUMN = "PAYMENT_TYPE_ID";
	public static final String PAYMENT_DESC_COLUMN = "PAYMENT_DESC";
	
    public BillingInfo mapFieldSet(FieldSet fieldSet) {
        BillingInfo info = new BillingInfo();

        info.setPaymentId(fieldSet.readString(PAYMENT_TYPE_ID_COLUMN));
        info.setPaymentDesc(fieldSet.readString(PAYMENT_DESC_COLUMN));

        return info;
    }
}
