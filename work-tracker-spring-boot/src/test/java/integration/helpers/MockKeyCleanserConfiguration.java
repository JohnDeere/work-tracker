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


package integration.helpers;

import com.deere.isg.worktracker.spring.KeyCleanser;
import com.deere.isg.worktracker.spring.PathMetadataCleanser;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MockKeyCleanserConfiguration extends MockWorkConfiguration {
    @Override
    public KeyCleanser keyCleanser() {
        PathMetadataCleanser keyCleanser = new PathMetadataCleanser();
        keyCleanser.setTransformFunction(this::transformString);

        keyCleanser.addBanned("transform", "unknown_transform");

        keyCleanser.addStandard("aId", "ant_id");
        keyCleanser.addStandard("bId", "bear_id");
        keyCleanser.addStandard("cId", "cat_id");
        keyCleanser.addStandard("dId", "dog_id");
        return keyCleanser;
    }

    private String transformString(String value) {
        return value.replaceAll("test", "mock");
    }
}
