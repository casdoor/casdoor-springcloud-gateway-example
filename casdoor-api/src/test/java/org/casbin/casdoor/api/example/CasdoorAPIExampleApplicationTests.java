// Copyright 2021 The casbin Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package org.casbin.casdoor.api.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CasdoorAPIExampleApplicationTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void resourcesNeedAnAccessToken() throws Exception {
        mvc.perform(get("/resource/getResource")).andExpect(status().isUnauthorized());
    }

    @Test
    void resourcesAreReturnedToTheUserOfTheToken() throws Exception {
        mvc.perform(get("/resource/getResource").with(jwt().jwt(token -> token
                        .claim("owner", "casbin")
                        .claim("name", "alice"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("casbin/alice got resource0"));
        mvc.perform(post("/resource/updateResource").with(jwt().jwt(token -> token
                        .claim("owner", "casbin")
                        .claim("name", "alice"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("casbin/alice updated, resource now is: resource1"));
    }
}
