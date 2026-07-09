package es.us.isa.httpmutator.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Normalizes and matches body mutation paths such as {@code Body/user/id}.
 */
public final class BodyPathIgnoreMatcher {

    private static final String BODY_ROOT = "Body";
    private static final String BODY_PREFIX = BODY_ROOT + "/";

    private BodyPathIgnoreMatcher() {
        // utility class
    }

    public static List<String> parsePaths(String rawPaths) {
        if (rawPaths == null || rawPaths.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String[] parts = rawPaths.split(",");
        List<String> paths = new ArrayList<String>();
        for (String part : parts) {
            paths.add(part);
        }
        return normalizePaths(paths);
    }

    public static List<String> normalizePaths(Collection<String> paths) {
        if (paths == null || paths.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> normalized = new LinkedHashSet<String>();
        for (String path : paths) {
            String normalizedPath = normalizePath(path);
            if (normalizedPath != null) {
                normalized.add(normalizedPath);
            }
        }
        return Collections.unmodifiableList(new ArrayList<String>(normalized));
    }

    public static String normalizePath(String path) {
        if (path == null) {
            return null;
        }

        String normalized = path.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.isEmpty()) {
            return null;
        }
        if (BODY_ROOT.equals(normalized) || normalized.startsWith(BODY_PREFIX)) {
            return normalized;
        }
        return BODY_PREFIX + normalized;
    }

    public static boolean matches(String candidatePath, Collection<String> ignoredPaths) {
        if (ignoredPaths == null || ignoredPaths.isEmpty()) {
            return false;
        }

        String normalizedCandidate = normalizePath(candidatePath);
        if (normalizedCandidate == null) {
            return false;
        }

        for (String ignoredPath : ignoredPaths) {
            String normalizedIgnored = normalizePath(ignoredPath);
            if (normalizedIgnored == null) {
                continue;
            }
            if (normalizedCandidate.equals(normalizedIgnored)
                    || normalizedCandidate.startsWith(normalizedIgnored + "/")) {
                return true;
            }
        }
        return false;
    }
}
