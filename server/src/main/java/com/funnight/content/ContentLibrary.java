package com.funnight.content;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The text content the games draw from. Each file under {@code resources/content} holds one
 * entry per line; blank lines and lines starting with # are ignored.
 */
@Component
public class ContentLibrary {
    private final List<String> mildDares = load("content/dares-mild.txt");
    private final List<String> wildDares = load("content/dares-wild.txt");
    private final List<String> guiltyPrompts = load("content/guilty-prompts.txt");

    public List<String> mildDares() { return mildDares; }
    public List<String> wildDares() { return wildDares; }
    public List<String> guiltyPrompts() { return guiltyPrompts; }

    private static List<String> load(String path) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8))) {
            List<String> lines = reader.lines().map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .toList();
            if (lines.isEmpty()) throw new IllegalStateException(path + " has no entries");
            return lines;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + path, e);
        }
    }
}
