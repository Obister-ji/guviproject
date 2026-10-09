package shop;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    final int id;
    String name;
    String email;
    Role role;
    String address = "";
    String paymentMethod = "Cash on delivery";

    // buyer-only state
    final Map<Integer, Double> wishlist = new LinkedHashMap<>(); // product id -> price when added
    final LinkedList<Integer> recentlyViewed = new LinkedList<>();

    private final String salt;
    private String hash;

    User(int id, String name, String email, String password, Role role) {
        this.id = id;
        this.name = name;
        this.email = email.toLowerCase();
        this.role = role;
        byte[] s = new byte[12];
        new SecureRandom().nextBytes(s);
        this.salt = Base64.getEncoder().encodeToString(s);
        this.hash = digest(salt, password);
    }

    boolean passwordMatches(String password) {
        return hash.equals(digest(salt, password));
    }

    void viewed(int productId) {
        recentlyViewed.remove((Integer) productId);
        recentlyViewed.addFirst(productId);
        while (recentlyViewed.size() > 10) {
            recentlyViewed.removeLast();
        }
    }

    private static String digest(String salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest((salt + password).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
