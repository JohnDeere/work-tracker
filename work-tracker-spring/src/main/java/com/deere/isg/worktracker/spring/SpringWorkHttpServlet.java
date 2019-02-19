package com.deere.isg.worktracker.spring;

import com.deere.isg.outstanding.Outstanding;
import com.deere.isg.worktracker.servlet.HttpWork;
import com.deere.isg.worktracker.servlet.WorkHttpServlet;
import com.deere.isg.worktracker.servlet.WorkSummary;

import java.util.List;
import java.util.stream.Collectors;

public class SpringWorkHttpServlet extends WorkHttpServlet {
    @Override
    protected List<WorkSummary<? extends HttpWork>> mapOutstandingToSummaryList() {
        return getOutstanding().stream().map(SpringWorkSummary::new).collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Outstanding<SpringWork> getOutstanding() {
        return (Outstanding<SpringWork>) super.getOutstanding();
    }

    private class SpringWorkSummary extends WorkSummary<SpringWork> {
        public SpringWorkSummary(SpringWork work) {
            super(work);
            setService(work.getEndpoint());
        }
    }
}
