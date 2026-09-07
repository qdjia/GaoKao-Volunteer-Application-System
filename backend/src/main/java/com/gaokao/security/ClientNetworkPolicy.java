package com.gaokao.security;

import jakarta.servlet.http.HttpServletRequest;
import com.gaokao.config.SecurityProperties;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class ClientNetworkPolicy {
    private static final String LOCAL_PROXY_HEADER = "X-Gaokao-Local-Proxy";
    private final byte[] proxySecret;

    public ClientNetworkPolicy(SecurityProperties properties) {
        String configured = properties.getProxySecret();
        this.proxySecret = configured == null ? new byte[0] : configured.getBytes(StandardCharsets.UTF_8);
    }

    public ClientContext describe(HttpServletRequest request) {
        String forwardedAddress = forwardedAddress(request);
        String effectiveAddress = forwardedAddress == null ? request.getRemoteAddr() : forwardedAddress;
        boolean local = (isLoopback(effectiveAddress)
                && isLoopback(request.getRemoteAddr())
                && isLoopback(request.getServerName())) || isTrustedLocalProxy(request);
        return new ClientContext(local, hash(effectiveAddress), hash(request.getHeader("User-Agent")));
    }

    private boolean isTrustedLocalProxy(HttpServletRequest request) {
        if (proxySecret.length < 32 || !isPrivateAddress(request.getRemoteAddr()) || !isLoopback(request.getServerName()))
            return false;
        String supplied = request.getHeader(LOCAL_PROXY_HEADER);
        return supplied != null && MessageDigest.isEqual(proxySecret, supplied.getBytes(StandardCharsets.UTF_8));
    }

    public void requireLocal(HttpServletRequest request) {
        if (!describe(request).local()) {
            throw new SecurityException("管理功能仅允许从服务器本机访问");
        }
    }

    private String forwardedAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("Forwarded");
        if (forwarded != null && !forwarded.isBlank()) {
            for (String part : forwarded.split(";")) {
                String value = part.trim();
                if (value.toLowerCase(Locale.ROOT).startsWith("for=")) {
                    return normalize(value.substring(4));
                }
            }
            return "invalid-forwarded-header";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return normalize(xForwardedFor.split(",", 2)[0]);
        }
        return null;
    }

    private String normalize(String address) {
        String value = address == null ? "" : address.trim().replace("\"", "");
        if (value.startsWith("[") && value.contains("]")) {
            return value.substring(1, value.indexOf(']'));
        }
        int colon = value.indexOf(':');
        if (colon > 0 && value.indexOf(':', colon + 1) < 0) {
            return value.substring(0, colon);
        }
        return value;
    }

    private boolean isLoopback(String address) {
        if (address == null || address.isBlank()) {
            return false;
        }
        try {
            return InetAddress.getByName(address).isLoopbackAddress();
        } catch (UnknownHostException ignored) {
            return false;
        }
    }

    private boolean isPrivateAddress(String address) {
        if (address == null || address.isBlank()) return false;
        try {
            InetAddress parsed = InetAddress.getByName(address);
            return parsed.isSiteLocalAddress() || parsed.isLinkLocalAddress();
        } catch (UnknownHostException ignored) {
            return false;
        }
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256不可用", e);
        }
    }

    public record ClientContext(boolean local, String ipHash, String userAgentHash) {
    }
}
