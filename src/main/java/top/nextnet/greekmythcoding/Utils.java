package top.nextnet.greekmythcoding;


public class Utils {
    final private static int[] validUriChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~:/?#[]@!$&'()*+,;=".chars().toArray();

    public static String sanitizeURI(String uri) {

        StringBuilder sb = new StringBuilder();
        for (int i : uri.chars().toArray()) {

            boolean found = false;
            for (int j : validUriChars) {
                if (i == j) {
                    found = true;
                    break;
                }
            }
            sb.append(found ? (char) i : '_');
        }
        return sb.toString();

    }
}
