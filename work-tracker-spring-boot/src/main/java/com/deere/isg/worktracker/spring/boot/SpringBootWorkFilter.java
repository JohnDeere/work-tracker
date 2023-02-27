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

package com.deere.isg.worktracker.spring.boot;

import com.deere.isg.worktracker.spring.AbstractSpringWorkFilter;
import com.deere.isg.worktracker.spring.SpringWork;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletRequest;
import java.util.function.Function;

/**
 * Provides a convenient way to just pass the type for the Work to spin up
 * a Work Filter
 *
 * @param <W> The type of work. Should extend {@link SpringWork}
 */
public class SpringBootWorkFilter<W extends SpringWork> extends AbstractSpringWorkFilter<W> {
    @Autowired
    private Function<ServletRequest, W> workFactory;

    @Override
    protected W createWork(ServletRequest request) {
        return workFactory.apply(request);
    }

    void setWorkFactory(Function<ServletRequest, W> workFactory) {
        this.workFactory = workFactory;
    }
}
