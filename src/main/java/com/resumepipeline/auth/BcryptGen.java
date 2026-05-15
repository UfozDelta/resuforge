package com.resumepipeline.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Standalone main for generating BCrypt password hashes.
 *
 * Usage (from project root):
 *   mvn -q compile
 *   java -cp target/classes;[deps] com.resumepipeline.auth.BcryptGen mypassword
 *
 * Or simpler, via Maven exec plugin if you add it. Easiest day-1 path is to call
 * this class once from a test, then paste the printed hash into application-local.yml
 * under auth.password-hash.
 */
public class BcryptGen {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: BcryptGen <plaintext-password>");
            System.exit(1);
        }
        String hash = new BCryptPasswordEncoder().encode(args[0]);
        System.out.println(hash);
    }
}
