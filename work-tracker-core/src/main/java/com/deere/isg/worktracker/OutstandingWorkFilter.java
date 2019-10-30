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

package com.deere.isg.worktracker;

import com.deere.isg.outstanding.Outstanding;

import java.util.Optional;
import java.util.stream.Stream;

public class OutstandingWorkFilter<W extends Work> implements OutstandingWorkTracker<W> {
    private OutstandingWorkTracker<? super W> parent;
    private Class<W> clazz;

    public OutstandingWorkFilter(OutstandingWorkTracker<? super W> parent, Class<W> clazz) {
        this.parent = parent;
        this.clazz = clazz;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Stream<W> stream() {
        return (Stream<W>) parent.stream().filter(this::isExpectedWork);
    }

    private boolean isExpectedWork(Work w) {
        return clazz.isAssignableFrom(w.getClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<W> current() {
        return (Optional<W>) parent.current().filter(this::isExpectedWork);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Outstanding<W>.Ticket create(W payload) {
        return (Outstanding<W>.Ticket) parent.create(payload);
    }
}
