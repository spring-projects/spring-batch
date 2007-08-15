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

package org.springframework.batch.sample.module.process;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.sample.dao.TradeWriter;
import org.springframework.batch.sample.domain.Trade;



public class TradeProcessor implements ItemProcessor {
    private static Log log = LogFactory.getLog(TradeProcessor.class);
    private TradeWriter writer;

    public void process(Object data) {
        if (!(data instanceof Trade)) {
            log.warn("TradeProcessor can process only Trade objects, skipping record");

            return;
        }

        Trade trade = (Trade) data;
        log.debug(data);

        //TODO put some processing of the trade object here
        writer.writeTrade(trade);
    }

    public void setWriter(TradeWriter dao) {
        this.writer = dao;
    }

	public void close() {
	}

	public void init() {
	}
}
