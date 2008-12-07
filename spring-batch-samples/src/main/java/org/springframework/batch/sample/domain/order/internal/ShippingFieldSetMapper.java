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
import org.springframework.batch.sample.domain.order.ShippingInfo;



public class ShippingFieldSetMapper implements FieldSetMapper<ShippingInfo> {
	
    public static final String ADDITIONAL_SHIPPING_INFO_COLUMN = "ADDITIONAL_SHIPPING_INFO";
	public static final String SHIPPING_TYPE_ID_COLUMN = "SHIPPING_TYPE_ID";
	public static final String SHIPPER_ID_COLUMN = "SHIPPER_ID";

	public ShippingInfo mapFieldSet(FieldSet fieldSet) {
        ShippingInfo info = new ShippingInfo();

        info.setShipperId(fieldSet.readString(SHIPPER_ID_COLUMN));
        info.setShippingTypeId(fieldSet.readString(SHIPPING_TYPE_ID_COLUMN));
        info.setShippingInfo(fieldSet.readString(ADDITIONAL_SHIPPING_INFO_COLUMN));

        return info;
    }
}
