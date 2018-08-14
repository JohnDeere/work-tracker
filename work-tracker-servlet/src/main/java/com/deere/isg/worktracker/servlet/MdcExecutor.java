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


package com.deere.isg.worktracker.servlet;

import com.deere.isg.worktracker.ContextualExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class MdcExecutor extends ContextualExecutor {
    public static final String PARENT_ENDPOINT = "parent_endpoint";
    public static final String PARENT_PATH = "parent_path";

    private Logger logger = LoggerFactory.getLogger(MdcExecutor.class);

    public MdcExecutor(Executor executor) {
        super(executor);
        setLogger(logger);
    }

    @Override
    protected Map<String, String> transformKeys(Map<String, String> parentMdc) {
        Map<String, String> copy = new HashMap<>(parentMdc);
        copy.put(PARENT_ENDPOINT, copy.remove("endpoint"));
        copy.put(PARENT_PATH, copy.remove(HttpWork.PATH));
        return copy;
    }
}
