/*
 * Copyright 2011 the original author or authors.
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
package org.springframework.batch.item.excel.transform;

import org.springframework.batch.item.excel.Sheet;
import org.springframework.batch.item.file.transform.FieldSet;

/**
 * Interface that is used by framework to convert a Cell[] into a {@link FieldSet}.
 * 
 * @author Marten Deinum 
 */

public interface RowTokenizer {

    FieldSet tokenize(Sheet sheet, String[] row);
}
