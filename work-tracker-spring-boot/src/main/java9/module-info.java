/**
 * Copyright 2019 Deere & Company
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

// Unpublished Work (c) 2019 Deere & Company

open module com.deere.isg.worktracker.spring.boot {
    requires com.deere.isg.worktracker.spring;
    requires spring.boot.starter.web;
    requires com.fasterxml.jackson.databind;
    exports com.deere.isg.worktracker.spring.boot;
}