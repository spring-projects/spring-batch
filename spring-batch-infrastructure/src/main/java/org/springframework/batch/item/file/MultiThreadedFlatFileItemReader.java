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

package org.springframework.batch.item.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.util.ExecutionContextUserSupport;
import org.springframework.util.ClassUtils;

/**
 * A multi-threaded aware {@link FlatFileItemReader} implementation
 * that uses start and end boundaries to delimit the portion of the
 * file that should be read.
 * <p/>
 * Reads all the file by default.
 *
 * @author Stephane Nicoll
 */
public class MultiThreadedFlatFileItemReader<T> extends FlatFileItemReader<T> {

    // Protected field would alleviate copy/paste here
    private static final String READ_COUNT = "read.count";

    private final Logger logger = LoggerFactory.getLogger(MultiThreadedFlatFileItemReader.class);

    private int startAt = 0;
    private int itemsCount = Integer.MAX_VALUE;

    // Would be better if the base ecSupport was protected somehow.
    private final ExecutionContextUserSupport ecSupport;

    public MultiThreadedFlatFileItemReader() {
        this.ecSupport = new ExecutionContextUserSupport();
        setName(ClassUtils.getShortName(MultiThreadedFlatFileItemReader.class));
    }


    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        super.open(executionContext);

        /*
         Since we are dealing with multiple chunk in the same area, let's make
         sure we will jump to the right item.

         The problem is that the maxItemCount and currentItemCount do not take this
         notion into account. Say a chunk starts at item #1000 and must read 100
         elements, the currentItemCount should be 1000 at beginning (ok) but the
         maxItemCount must be 1100 (and not 100 like it should be)
        */
        if (!executionContext.containsKey(ecSupport.getKey(READ_COUNT))) {
            // We need to jump and this is a fresh start (nothing in the context)
            if (startAt > 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Skipping to item [" + startAt + "]");
                }
                // Make sure to register the maxItemCount properly
                final int maxItemCount = startAt + itemsCount;
                setMaxItemCount(maxItemCount);
                try {
                    for (int i = 0; i < startAt; i++) {
                        read();
                    }
                } catch (Exception e) {
                    throw new ItemStreamException(
                            "Could not move to stored position on restart", e);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Ready to read from [" + getCurrentItemCount() + "] to [" + maxItemCount + "]");
                }
            } else {
                // Fresh start on the first item so let's state the max item count is simply the
                // itemsCount
                setMaxItemCount(itemsCount);
                if (logger.isDebugEnabled()) {
                    logger.debug("Ready to read from [" + getCurrentItemCount() + "] to [" + itemsCount + "]");
                }
            }
        }
    }

    /**
     * Sets the item number at which this instance should start reading. Set
     * to 0 by default so that this instance starts at the first item.
     *
     * @param startAt the number of the item at which this instance should
     * start reading
     */
    public void setStartAt(int startAt) {
        this.startAt = startAt;
    }

    /**
     * Sets the number of items this instance should read.
     *
     * @param itemsCount the number of items to read
     */
    public void setItemsCount(int itemsCount) {
        this.itemsCount = itemsCount;
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        // default constructor of the parent calls this before the instance is
        // actually initialized. With a protected ecSupport, this can go away
        // altogether.
        if (ecSupport != null) {
            ecSupport.setName(name);
        }
    }
}

