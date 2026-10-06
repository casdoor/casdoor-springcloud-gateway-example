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
package org.casbin.casdoor.gateway.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockOidcLogin;

@SpringBootTest
@AutoConfigureWebTestClient
class CasdoorGatewayExampleApplicationTests {

    @Autowired
    private WebTestClient webClient;

    @Test
    void indexIsPublic() {
        webClient.get().uri("/").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body).contains("Login with Casdoor"));
    }

    @Test
    void apisNeedASignedInUser() {
        webClient.get().uri("/api/resource/getResource").exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void loginRedirectsToCasdoor() {
        webClient.get().uri("/oauth2/authorization/casdoor").exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location",
                        location -> assertThat(location).startsWith("https://door.casdoor.com/login/oauth/authorize?"));
    }

    @Test
    void indexShowsTheSignedInUser() {
        OidcIdToken idToken = OidcIdToken.withTokenValue("id-token")
                .subject("1234")
                .claim("preferred_username", "alice")
                .build();
        // casdoor-spring-boot-starter names the user by preferred_username, the Casdoor username
        webClient.mutateWith(mockOidcLogin().oidcUser(new DefaultOidcUser(List.of(), idToken, "preferred_username")))
                .get().uri("/").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body).contains("Signed in as <b>alice</b>"));
    }
}
