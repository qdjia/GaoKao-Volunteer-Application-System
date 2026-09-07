package com.gaokao.security;

import com.gaokao.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientNetworkPolicyTest {
    private static final String SECRET = "local-proxy-secret-at-least-32-bytes";
    private final SecurityProperties properties = properties();
    private final ClientNetworkPolicy policy = new ClientNetworkPolicy(properties);

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

    @Test
    void acceptsPrivateComposeProxyOnlyWithSecretAndLoopbackHost() {
        MockHttpServletRequest request = request("172.20.0.3", "localhost");
        request.addHeader("X-Forwarded-For", "172.20.0.1");
        request.addHeader("X-Gaokao-Local-Proxy", SECRET);

        assertThat(policy.describe(request).local()).isTrue();
    }

    @Test
    void rejectsWrongSecretPublicProxyAndNonLoopbackHost() {
        MockHttpServletRequest wrongSecret = request("172.20.0.3", "localhost");
        wrongSecret.addHeader("X-Gaokao-Local-Proxy", "wrong");
        MockHttpServletRequest publicProxy = request("203.0.113.10", "localhost");
        publicProxy.addHeader("X-Gaokao-Local-Proxy", SECRET);
        MockHttpServletRequest publicHost = request("172.20.0.3", "demo.example.com");
        publicHost.addHeader("X-Gaokao-Local-Proxy", SECRET);

        assertThat(policy.describe(wrongSecret).local()).isFalse();
        assertThat(policy.describe(publicProxy).local()).isFalse();
        assertThat(policy.describe(publicHost).local()).isFalse();
    }

    @Test
    void ignoresProxyHeaderWhenServerSecretIsNotConfigured() {
        MockHttpServletRequest request = request("172.20.0.3", "localhost");
        request.addHeader("X-Gaokao-Local-Proxy", SECRET);

        assertThat(new ClientNetworkPolicy(new SecurityProperties()).describe(request).local()).isFalse();
    }

    private MockHttpServletRequest request(String remoteAddress, String serverName) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        request.setServerName(serverName);
        return request;
    }

    private SecurityProperties properties() {
        SecurityProperties value = new SecurityProperties();
        value.setProxySecret(SECRET);
        return value;
    }
}
