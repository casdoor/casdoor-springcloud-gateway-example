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
package org.casbin.casdoor.gateway.example.controller;

import javax.annotation.Resource;
import org.casbin.casdoor.entity.CasdoorUser;
import org.casbin.casdoor.exception.CasdoorAuthException;
import org.casbin.casdoor.service.CasdoorAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @author conghuhu
 */
@Controller
public class FrontController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrontController.class);

    @Resource
    private CasdoorAuthService casdoorAuthService;

    @RequestMapping("toLogin")
    public Mono<String> toLogin() {
        return Mono.just("toLogin");
    }

    @RequestMapping("login")
    public Mono<String> login() {
        return Mono.just("redirect:" + casdoorAuthService.getSigninUrl("http://localhost:9090/callback"));
    }

    @RequestMapping("/")
    public Mono<String> index() {
        return Mono.just("index");
    }

    @RequestMapping("callback")
    public Mono<String> callback(String code, String state, ServerWebExchange exchange) {
        String token = "";
        CasdoorUser user = null;
        try {
            token = casdoorAuthService.getOAuthToken(code, state);
            user = casdoorAuthService.parseJwtToken(token);
        } catch (CasdoorAuthException e) {
            e.printStackTrace();
        }
        CasdoorUser finalUser = user;
        return exchange.getSession().flatMap(session -> {
            session.getAttributes().put("casdoorUser", finalUser);
            return Mono.just("redirect:/");
        });
    }

    @RequestMapping("logout")
    public Mono<String> logout(ServerWebExchange exchange) {
        return exchange.getSession().flatMap(session -> {
            session.getAttributes().remove("casdoorUser");
            return Mono.just("toLogin");
        });
    }

}
