package com.cyberrange.backend;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.Console;

public class PasswordHashGenerator {

    public static void main(String[] args) {

        Console console = System.console();

        if (console == null) {
            System.out.println(
                    "Please run this program in a terminal."
            );
            return;
        }

        char[] password = console.readPassword(
                "Enter your admin password: "
        );

        if (password == null || password.length == 0) {
            System.out.println("Password cannot be empty.");
            return;
        }

        try {
            PasswordEncoder encoder = new BCryptPasswordEncoder();

            String rawPassword = new String(password);
            String passwordHash = encoder.encode(rawPassword);

            System.out.println("\nBCrypt hash:");
            System.out.println(passwordHash);

            System.out.println("\nVerification: "
                    + encoder.matches(rawPassword, passwordHash));

        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }
}
