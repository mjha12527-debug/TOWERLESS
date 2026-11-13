package com.towerless.app.ui;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;

/** Official destinations must be filled only after the project owner verifies them. */
final class OfficialLinks {
    // Intentionally empty until verified official destinations are supplied.
    static final String RELEASE_URL = "";
    static final String PROJECT_URL = "";
    private static final Set<String> PROJECT_HOSTS = new HashSet<>();

    static {
        PROJECT_HOSTS.add("github.com");
        PROJECT_HOSTS.add("huggingface.co");
    }

    private OfficialLinks() { }

    static boolean isValidReleaseUrl(String value) {
        URI uri = parseStrictHttps(value);
        if (uri == null || !"github.com".equals(uri.getHost().toLowerCase(Locale.ROOT))) return false;

        String rawPath = uri.getRawPath();
        if (rawPath == null) return false;
        String[] path = rawPath.split("/", -1);
        return path.length >= 4
                && isGitHubPathSegment(path[1])
                && isGitHubPathSegment(path[2])
                && "releases".equals(path[3])
                && !containsDotPathSegment(uri.getPath());
    }

    static boolean isValidProjectUrl(String value) {
        URI uri = parseStrictHttps(value);
        return uri != null && PROJECT_HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT));
    }

    private static URI parseStrictHttps(String value) {
        if (value == null || value.isEmpty() || containsWhitespaceOrControl(value)) return null;
        try {
            URI uri = new URI(value);
            if (!uri.isAbsolute() || uri.isOpaque()
                    || !"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getRawUserInfo() != null
                    || uri.getHost() == null
                    || uri.getHost().isEmpty()) return null;

            String authority = uri.getRawAuthority();
            String host = uri.getHost();
            if (authority == null
                    || !(authority.equalsIgnoreCase(host)
                    || authority.equalsIgnoreCase(host + ":443"))) return null;

            int port = uri.getPort();
            if (port != -1 && port != 443) return null;
            return uri;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static boolean isGitHubPathSegment(String segment) {
        return segment != null && !".".equals(segment) && !"..".equals(segment)
                && segment.matches("[A-Za-z0-9_.-]+");
    }

    private static boolean containsDotPathSegment(String path) {
        if (path == null) return true;
        for (String segment : path.split("/", -1)) {
            if (".".equals(segment) || "..".equals(segment)) return true;
        }
        return false;
    }

    private static boolean containsWhitespaceOrControl(String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isWhitespace(ch) || Character.isSpaceChar(ch) || Character.isISOControl(ch)) {
                return true;
            }
        }
        return false;
    }
}
