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

package com.deere.isg.worktracker.servlet;

import com.deere.isg.worktracker.OutstandingWorkTracker;

/**
 * If your filter accepts generic types, you can extend this class instead
 * of {@link BaseFilter} to just pass the type and get the appropriate type
 * for your outstanding and floodSensor.
 *
 * @param <W> A generic type that extends HttpWork
 */
public abstract class BaseTypeFilter<W extends HttpWork> extends BaseFilter {
    @Override
    @SuppressWarnings("unchecked")
    public OutstandingWorkTracker<W> getOutstanding() {
        return (OutstandingWorkTracker<W>) super.getOutstanding();
    }

    @Override
    @SuppressWarnings("unchecked")
    public HttpFloodSensor<W> getFloodSensor() {
        return (HttpFloodSensor<W>) super.getFloodSensor();
    }
}
