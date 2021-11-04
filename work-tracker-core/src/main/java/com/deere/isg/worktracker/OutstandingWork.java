/**
 * Copyright 2021 Deere & Company
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

public class OutstandingWork<W extends Work> extends Outstanding<W> implements OutstandingWorkTracker<W> {
    private InheritableThreadLocal<PayloadHolder<W>> currentPayload = new InheritableThreadLocal<>();

    @Override
    protected Ticket createTicket(W payload) {
        ThreadTrackedTicket ticket = new ThreadTrackedTicket(payload);
        if (currentPayload != null) {
            currentPayload.set(ticket.holder);
        }
        return ticket;
    }

    public Optional<W> current() {
        return Optional.ofNullable(currentPayload.get()).map(PayloadHolder::getPayload);
    }

    private static class PayloadHolder<P> {
        private volatile P payload;
        PayloadHolder(P payload) {
            this.payload = payload;
        }
        void clearPayload() {
            this.payload = null;
        }
        P getPayload() {
            return payload;
        }
    }

    private class ThreadTrackedTicket extends Outstanding<W>.Ticket {
        private final PayloadHolder<W> holder;

        ThreadTrackedTicket(W payload) {
            this.holder = new PayloadHolder<>(payload);
        }

        public Optional<W> getPayload() {
            return Optional.ofNullable(holder.getPayload());
        }

        public boolean isClosed() {
            return this.holder.getPayload() == null;
        }

        public void clearPayload() {
            holder.clearPayload();
        }
    }
}
