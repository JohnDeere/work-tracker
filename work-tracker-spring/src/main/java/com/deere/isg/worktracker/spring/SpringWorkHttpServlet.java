/**
 * Copyright 2018-2021 Deere & Company
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

package com.deere.isg.worktracker.spring;

import com.deere.isg.worktracker.Work;
import com.deere.isg.worktracker.servlet.WorkHttpServlet;
import com.deere.isg.worktracker.servlet.WorkSummary;

import java.util.List;

/**
 * This class once did some work, then its responsibilities moved elsewhere.
 * However, there are a bunch of apps with it configured, and its possible it may
 * come into use again, so we're not deprecating it just yet.
 */
public class SpringWorkHttpServlet extends WorkHttpServlet {
    @Override
    protected List<WorkSummary<? extends Work>> mapOutstandingToSummaryList() {
        return super.mapOutstandingToSummaryList();
    }
}
