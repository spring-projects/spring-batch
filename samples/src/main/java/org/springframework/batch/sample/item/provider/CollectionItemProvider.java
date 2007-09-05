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

package org.springframework.batch.sample.item.provider;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.FieldSetInputSource;
import org.springframework.batch.io.file.FieldSetMapper;
import org.springframework.batch.item.provider.AbstractItemProvider;


public class CollectionItemProvider extends AbstractItemProvider {
	
	private static final Log log = LogFactory.getLog(CollectionItemProvider.class);
	
    private FieldSetInputSource inputSource;

    //collects simple records
    private Collection multiRecord;

    //marks we have finished reading one whole multiRecord
    private boolean recordFinished;

    //mapps a sigle line to a simple record
    private FieldSetMapper fieldSetMapper;

    public Object next() {
        recordFinished = false;

        while (!recordFinished) {
            process(inputSource.readFieldSet());
        }

        if (multiRecord != null) {
            Collection result = new ArrayList(multiRecord);
            multiRecord = null;

            return result;
        } else {
            return null;
        }
    }

    private void process(FieldSet fieldSet) {
        //finish processing if we hit the end of file
        if (fieldSet == null) {
            log.debug("FINISHED");
            recordFinished = true;
            multiRecord = null;

            return;
        }

        //start a new collection
        if (fieldSet.readString(0).equals("BEGIN")) {
        	log.debug("STARTING NEW RECORD");
            multiRecord = new ArrayList();

            return;
        }

        //mark we are finished with current collection
        if (fieldSet.readString(0).equals("END")) {
        	log.debug("END OF RECORD");
            recordFinished = true;

            return;
        }

        //add a simple record to the current collection
        log.debug("MAPPING: " + fieldSet);
        multiRecord.add(fieldSetMapper.mapLine(fieldSet));
    }

    public void setInputSource(FieldSetInputSource inputTemplate) {
        this.inputSource = inputTemplate;
    }

    public void setFieldSetMapper(FieldSetMapper mapper) {
        this.fieldSetMapper = mapper;
    }

}
