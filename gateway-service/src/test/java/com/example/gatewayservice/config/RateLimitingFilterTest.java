package com.example.gatewayservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the one thing that matters most about a rate limiter that sits in front of
 * login: it must actually limit abusive traffic, it must never touch unrelated
 * routes, and different callers (IPs) must never affect each other's limit.
 */
class RateLimitingFilterTest {

    @Test
    void allowsRequestsUnderTheLimit() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();

        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(loginRequest("10.0.0.1"), response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void blocksTheRequestThatExceedsTheWindowLimit() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        MockHttpServletResponse last = null;

        for (int i = 0; i < 21; i++) {
            last = new MockHttpServletResponse();
            filter.doFilter(loginRequest("10.0.0.2"), last, new MockFilterChain());
        }

        assertThat(last.getStatus()).isEqualTo(429);
        assertThat(last.getContentAsString()).contains("Çox tez-tez");
    }

    @Test
    void neverLimitsRoutesOtherThanLoginAndRegister() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();

        for (int i = 0; i < 50; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/artists/public/search");
            request.setRemoteAddr("10.0.0.3");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    void oneIpBeingBlockedDoesNotAffectAnotherIp() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();

        for (int i = 0; i < 20; i++) {
            filter.doFilter(loginRequest("10.0.0.4"), new MockHttpServletResponse(), new MockFilterChain());
        }
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(loginRequest("10.0.0.4"), blocked, new MockFilterChain());
        assertThat(blocked.getStatus()).isEqualTo(429);

        MockHttpServletResponse stillAllowed = new MockHttpServletResponse();
        filter.doFilter(loginRequest("10.0.0.5"), stillAllowed, new MockFilterChain());
        assertThat(stillAllowed.getStatus()).isEqualTo(200);
    }

    private MockHttpServletRequest loginRequest(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr(ip);
        return request;
    }
}
