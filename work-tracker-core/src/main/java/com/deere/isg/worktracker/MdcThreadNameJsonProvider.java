/**
 * Copyright 2018-2023 Deere & Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.deere.isg.worktracker;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.composite.loggingevent.ThreadNameJsonProvider;

import java.io.IOException;

import static com.deere.isg.worktracker.StringUtils.isNotBlank;

public class MdcThreadNameJsonProvider extends ThreadNameJsonProvider {
    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        String mdcThreadName = event.getMDCPropertyMap().get(FIELD_THREAD_NAME);
        String threadName = isNotBlank(mdcThreadName) ? mdcThreadName : event.getThreadName();
        JsonWritingUtils.writeStringField(generator, getFieldName(), threadName);
    }
}
