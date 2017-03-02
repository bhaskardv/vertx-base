/*
 * Copyright 2016 The Board of Trustees of The Leland Stanford Junior University.
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
package com.github.susom.vertx.base;

import java.security.SecureRandom;
import java.util.function.Function;

import com.github.susom.database.Config;
import com.github.susom.database.ConfigInvalidException;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * Provide standard security services for protecting applications.
 *
 * @author garricko
 */
public class SecurityImpl implements Security {
  private Security delegate;

  public SecurityImpl(Vertx vertx, Router root, SecureRandom secureRandom, Function<String, String> cfg)
      throws Exception {

    // TODO connect to security coordinator and dynamically determine security providers and policies

    Config config = Config.from().custom(cfg::apply).get();

    if (config.getBooleanOrFalse("insecure.fake.security")) {
      delegate = new FakeAuthenticator(vertx, root, secureRandom, cfg);
    } else if (config.getString("security.authenticator", "saml").equals("saml")) {
      delegate = new SamlAuthenticator(vertx, root, secureRandom, cfg);
    } else if (config.getString("security.authenticator", "saml").equals("oidc-keycloak")) {
      delegate = new OidcKeycloakAuthenticator(vertx, root, secureRandom, cfg);
    } else if (config.getString("security.authenticator", "saml").equals("custom")) {
      delegate = new CustomAuthenticator();
    } else {
      throw new ConfigInvalidException("Set security.authenticator=[saml|oidc-keycloak|custom]");
    }
  }

  /**
   * Use this method to place web applications (browser-based) behind user
   * authentication.
   *
   * @param mountPoint path relative to the root router passed in the constructor
   * @return a secured router (authenticated, but no authorization checks)
   */
  @Override
  public Router authenticatedRouter(String mountPoint) {
    return delegate.authenticatedRouter(mountPoint);
  }

  /**
   * Create a handler that enforces an authorization check. The request
   * must already have been authenticated, or a 401 will be returned. If
   * authentication has occurred, but the specified authority has not
   * been granted, a 403 will be returned.
   *
   * @param authority what the authenticated subject is required to have been granted
   * @return a handler to place in front of the protected resources
   */
  @Override
  public Handler<RoutingContext> requireAuthority(String authority) {
    return delegate.requireAuthority(authority);
  }
}
