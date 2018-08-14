/**
 * Copyright 2018 Deere & Company
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

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import com.deere.isg.worktracker.MdcThreadNameJsonProvider
import com.deere.isg.worktracker.RootCauseTurboFilter
import com.deere.isg.worktracker.Work
import net.logstash.logback.composite.GlobalCustomFieldsJsonProvider
import net.logstash.logback.composite.loggingevent.*
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder

// This logback is for demonstration only

appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} %magenta([%thread]) %highlight(%-5level) %logger{36}.%M - user_id=%X{user_id} role=%X{role} - %msg%n"
    }
}

def APP_NAME = "spring-example"
appender("FILEOUTPUT", RollingFileAppender) {
    file = "logs/application.log"
    encoder(LoggingEventCompositeJsonEncoder) {
        providers(LoggingEventJsonProviders) {
            globalCustomFields(GlobalCustomFieldsJsonProvider) {
                customFields = "{\"app\": \"${APP_NAME}\"}"
            }
            message(MessageJsonProvider)
            mdc(MdcJsonProvider) {
                excludeMdcKeyName = Work.THREAD_NAME
            }
            arguments(ArgumentsJsonProvider)
            threadName(MdcThreadNameJsonProvider)
            logLevelValue(LogLevelValueJsonProvider)
            logLevel(LogLevelJsonProvider)
            stackTrace(StackTraceJsonProvider)
            stackHash(StackHashJsonProvider)
            timestamp(LoggingEventFormattedTimestampJsonProvider)
            callerData(CallerDataJsonProvider)
        }
    }
    rollingPolicy(TimeBasedRollingPolicy) {
        fileNamePattern = "logs/application.log.%d{yyyy-MM-dd}"
        maxHistory = 2
        cleanHistoryOnStart = true
    }
}

turboFilter(RootCauseTurboFilter)

root(INFO, ["STDOUT", "FILEOUTPUT"])
