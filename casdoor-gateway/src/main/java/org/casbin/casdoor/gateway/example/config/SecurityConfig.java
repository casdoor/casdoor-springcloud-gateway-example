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
package org.casbin.casdoor.gateway.example.config;

import org.casbin.casdoor.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.DelegatingServerAuthenticationEntryPoint.DelegateEntry;
import org.springframework.security.web.server.DelegatingServerAuthenticationEntryPoint;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.logout.DelegatingServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.SecurityContextServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, AuthService authService,
                                                         ServerOAuth2AuthorizedClientRepository authorizedClients) {
        // the APIs answer 401 to the page, other pages go to the Casdoor sign-in page
        DelegatingServerAuthenticationEntryPoint entryPoint = new DelegatingServerAuthenticationEntryPoint(
                new DelegateEntry(ServerWebExchangeMatchers.pathMatchers("/api/**"),
                        new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)));
        entryPoint.setDefaultEntryPoint(new RedirectServerAuthenticationEntryPoint("/oauth2/authorization/casdoor"));

        RedirectServerLogoutSuccessHandler logoutSuccessHandler = new RedirectServerLogoutSuccessHandler();
        logoutSuccessHandler.setLogoutSuccessUrl(URI.create("/"));

        http.authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/", "/error").permitAll()
                        .anyExchange().authenticated())
                // sign in with Casdoor, configured by casdoor-spring-boot-starter
                .oauth2Login(Customizer.withDefaults())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint))
                .logout(logout -> logout
                        .logoutHandler(new DelegatingServerLogoutHandler(
                                casdoorLogoutHandler(authService, authorizedClients),
                                new SecurityContextServerLogoutHandler()))
                        .logoutSuccessHandler(logoutSuccessHandler));
        return http.build();
    }

    /**
     * Also ends the user's session in Casdoor, so that signing in again asks for the password.
     */
    private ServerLogoutHandler casdoorLogoutHandler(AuthService authService,
                                                     ServerOAuth2AuthorizedClientRepository authorizedClients) {
        return (webFilterExchange, authentication) -> {
            if (!(authentication instanceof OAuth2AuthenticationToken token)) {
                return Mono.empty();
            }
            return authorizedClients.loadAuthorizedClient(token.getAuthorizedClientRegistrationId(), token,
                            webFilterExchange.getExchange())
                    .flatMap(client -> Mono.fromRunnable(() ->
                                    authService.logoutCurrentSession(client.getAccessToken().getTokenValue()))
                            .subscribeOn(Schedulers.boundedElastic()))
                    // the Casdoor session may already have ended
                    .onErrorResume(e -> Mono.empty())
                    .then();
        };
    }
}
