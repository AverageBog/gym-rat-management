package com.gymrat.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpaFallbackAdviceTest {

    private final SpaFallbackAdvice advice = new SpaFallbackAdvice();

    private static NoResourceFoundException ex(String path) {
        return new NoResourceFoundException(HttpMethod.GET, path);
    }

    private static HttpServletRequest request(String uri) {
        return new MockHttpServletRequest("GET", uri);
    }

    @Test
    void handleNoResource_spaRoute_forwardsToIndex() throws Exception {
        String view = advice.handleNoResource(ex("/members"), request("/members"));
        assertThat(view).isEqualTo("forward:/index.html");
    }

    @Test
    void handleNoResource_nestedSpaRoute_forwardsToIndex() throws Exception {
        String view = advice.handleNoResource(ex("/members/3/edit"), request("/members/3/edit"));
        assertThat(view).isEqualTo("forward:/index.html");
    }

    @Test
    void handleNoResource_rootPath_forwardsToIndex() throws Exception {
        String view = advice.handleNoResource(ex("/"), request("/"));
        assertThat(view).isEqualTo("forward:/index.html");
    }

    @Test
    void handleNoResource_apiPath_rethrows() {
        assertThatThrownBy(() -> advice.handleNoResource(ex("/api/missing"), request("/api/missing")))
                .isInstanceOf(NoResourceFoundException.class);
    }

    @Test
    void handleNoResource_h2ConsolePath_rethrows() {
        assertThatThrownBy(() -> advice.handleNoResource(ex("/h2-console"), request("/h2-console")))
                .isInstanceOf(NoResourceFoundException.class);
    }

    @Test
    void handleNoResource_h2ConsoleSubPath_rethrows() {
        assertThatThrownBy(() -> advice.handleNoResource(ex("/h2-console/login.do"), request("/h2-console/login.do")))
                .isInstanceOf(NoResourceFoundException.class);
    }

    @Test
    void handleNoResource_pathWithJsExtension_rethrows() {
        // The original bug: a previous catch-all incorrectly forwarded /assets/foo.js
        // to /index.html, breaking SPA asset loading. This guards against regression.
        assertThatThrownBy(() -> advice.handleNoResource(ex("/assets/missing.js"), request("/assets/missing.js")))
                .isInstanceOf(NoResourceFoundException.class);
    }

    @Test
    void handleNoResource_pathWithCssExtension_rethrows() {
        assertThatThrownBy(() -> advice.handleNoResource(ex("/assets/missing.css"), request("/assets/missing.css")))
                .isInstanceOf(NoResourceFoundException.class);
    }

    @Test
    void handleNoResource_pathWithMultipleDots_rethrows() {
        assertThatThrownBy(() -> advice.handleNoResource(ex("/file.min.js"), request("/file.min.js")))
                .isInstanceOf(NoResourceFoundException.class);
    }
}
