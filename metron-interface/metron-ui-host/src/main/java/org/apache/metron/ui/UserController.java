/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.ui;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * A trivial endpoint to ping for currently authenticated user principal
 * 
 */
@RestController
public class UserController {
    @Value("${knox.sso.url}")
    private String knoxSSOUrl;

    @RequestMapping(path = "/whoami", method = RequestMethod.GET)
    public String user(Principal user) {
        return user.getName();
    }
    
    @Secured("IS_AUTHENTICATED_FULLY")
    @RequestMapping(path = "/whoami/roles", method = RequestMethod.GET)
    public List<String> user() {
      UserDetails userDetails = (UserDetails)SecurityContextHolder.getContext().
          getAuthentication().getPrincipal();
      return userDetails.getAuthorities().stream().map(ga -> ga.getAuthority()).collect(Collectors.toList());
    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public RedirectView logout(Principal user, HttpServletResponse httpServletResponse, @RequestParam("originalUrl") String originalUrl) throws UnsupportedEncodingException {
        StringBuilder redirect = new StringBuilder("redirect:" );
        redirect.append(knoxSSOUrl.replaceAll("websso", "webssout"));
        redirect.append(knoxSSOUrl.contains("?") ? "&": "?");
        redirect.append("originalUrl=");
        redirect.append(URLEncoder.encode(originalUrl, StandardCharsets.UTF_8.name()));

        return new RedirectView(redirect.toString());
    }
}
