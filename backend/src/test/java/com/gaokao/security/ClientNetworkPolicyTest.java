package com.gaokao.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientNetworkPolicyTest {
    private final ClientNetworkPolicy policy = new ClientNetworkPolicy();

    @Test
    void acceptsLoopbackRequestWithLocalHost() {
        MockHttpServletRequest request = request("127.0.0.1", "localhost");

        assertThat(policy.describe(request).local()).isTrue();
    }

    @Test
    void rejectsPublicForwardedAddressEvenBehindLocalProxy() {
        MockHttpServletRequest request = request("127.0.0.1", "localhost");
        request.addHeader("X-Forwarded-For", "203.0.113.10");

        assertThat(policy.describe(request).local()).isFalse();
    }

    @Test
    void rejectsTunnelHostEvenWhenProxyDoesNotForwardClientAddress() {
        MockHttpServletRequest request = request("127.0.0.1", "demo.example.com");

        assertThat(policy.describe(request).local()).isFalse();
    }

    private MockHttpServletRequest request(String remoteAddress, String serverName) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        request.setServerName(serverName);
        return request;
    }
}
