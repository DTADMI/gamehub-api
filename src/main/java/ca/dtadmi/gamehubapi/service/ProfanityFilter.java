package ca.dtadmi.gamehubapi.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class ProfanityFilter {
    private static final Set<String> BAD = new HashSet<>(Arrays.asList(
            // very small demo list; replace with a better dictionary later
            "badword", "curse"
    ));

    public String filter(String input) {
        if (input == null || input.isBlank()) return input;
        String[] parts = input.split("\\s+");
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            String lower = p.toLowerCase();
            if (BAD.contains(lower.replaceAll("[^a-z]", ""))) {
                parts[i] = mask(p);
            }
        }
        return String.join(" ", parts);
    }

    private String mask(String s) {
        if (s.length() <= 2) return "**";
        char[] chars = s.toCharArray();
        for (int i = 1; i < chars.length - 1; i++) {
            if (Character.isLetterOrDigit(chars[i])) chars[i] = '*';
        }
        return new String(chars);
    }
}