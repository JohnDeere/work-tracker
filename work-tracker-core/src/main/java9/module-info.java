/**
 * Copyright 2020 Deere & Company
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

open module com.deere.isg.worktracker.core {
    requires com.deere.isg.clock;
    requires transitive com.deere.isg.outstanding;
    requires slf4j.api;
    requires logstash.logback.encoder;
    requires com.fasterxml.jackson.databind;
    requires logback.core;
    requires logback.classic;
    requires java.annotation;
    requires com.fasterxml.jackson.core;
    requires oswego.concurrent;
    exports com.deere.isg.worktracker;
}