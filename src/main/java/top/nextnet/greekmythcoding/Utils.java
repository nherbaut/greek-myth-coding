package top.nextnet.greekmythcoding;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class Utils {
    final private static Set<Character> invalidURIChars = Arrays.asList("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~:/?#[]@!$&'()*+,;=".split(".")).stream().map(s -> s.charAt(0)).collect(Collectors.toSet());

    public static String sanitizeURI(String uri) {
        invalidURIChars.stream().forEach(c -> uri.replace(c, '_'));
        return uri;
    }
}
