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


package integration.helpers;

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.Work;
import com.deere.isg.worktracker.spring.SpringWork;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.MatrixVariable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@RestController
public class MockController {
    private OutstandingWork<SpringWork> outstanding;

    public MockController() {

    }

    public MockController(OutstandingWork<SpringWork> outstanding) {
        this.outstanding = outstanding;
    }

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<String> helloWorld() {
        return outstanding.current().map(Work::getLimits).orElse(Collections.emptySet());
    }

    @GetMapping(value = "/test/{id}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Map<String, String> testId(@PathVariable("id") String id) {
        return MDC.getCopyOfContextMap();
    }

    @GetMapping(value = "/user/{id}/role/{role}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Map<String, String> userRole(@PathVariable("id") String id, @PathVariable("role") String role) {
        return MDC.getCopyOfContextMap();
    }

    @GetMapping(value = "/query/{guid}/{matrixVar}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Map<String, String> queryList(@PathVariable("guid") String guid, @MatrixVariable(pathVar = "matrixVar") Map<String, String> start) {
        return MDC.getCopyOfContextMap();
    }

    @GetMapping("/oauth-token/{TOKEN}")
    public Map<String, String> oauthToken(@PathVariable("TOKEN") String token) {
        return MDC.getCopyOfContextMap();
    }

    @GetMapping("/sum/{first}/{second}")
    public Map<String, String> sum(@PathVariable("first") int first, @PathVariable("second") int second) {
        return MDC.getCopyOfContextMap();
    }

    @GetMapping("/userId/{id}/oauth-token/{token}/logic/{elapsedMS}")
    public Map<String, String> idWithToken(@PathVariable("id") String id,
            @PathVariable("token") String token,
            @PathVariable("elapsedMS") String time) {
        return MDC.getCopyOfContextMap();
    }

    @GetMapping("/aId/{id}/{bId}/{cId}/{dId}")
    public Map<String, String> antId(@PathVariable("id") String aId,
            @PathVariable("bId") String bId,
            @PathVariable("cId") String cId,
            @PathVariable("dId") String dId) {
        return MDC.getCopyOfContextMap();
    }

    @GetMapping("/transform/{transform}")
    public Map<String, String> transformsSomething(@PathVariable("transform") String something) {
        return MDC.getCopyOfContextMap();
    }
}
