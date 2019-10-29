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
import net.logstash.logback.argument.StructuredArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OutstandingWork<W extends Work> extends Outstanding<W> implements OutstandingWorkTracker<W> {
    private InheritableThreadLocal<Ticket> currentPayload = new InheritableThreadLocal<>();

    @Override
    protected Ticket createTicket(W payload) {
        Ticket ticket = super.createTicket(payload);
        if (currentPayload != null) {
            currentPayload.set(ticket);
        }
        return ticket;
    }

    public Optional<W> current() {
        return Optional.ofNullable(currentPayload.get()).flatMap(Ticket::getPayload);
    }

    public <E extends Throwable, E2 extends Throwable> void doInTransactionChecked(W payload, CheckedRunnable<E, E2> runnable) throws E, E2 {
        try (Outstanding<W>.Ticket ignored = this.create(payload)) {
            runnable.run();
        }
    }
}
