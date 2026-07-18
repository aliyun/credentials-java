package com.aliyun.credentials.utils;

import com.aliyun.credentials.exception.CredentialException;

import java.util.ArrayList;
import java.util.List;

/**
 * Split process_command into argv with quote support so paths containing spaces
 * (e.g. Windows "C:\\Program Files\\...") can be passed as a single argument.
 * Escape rules follow POSIX shlex semantics.
 */
public final class CommandLineUtils {
    private CommandLineUtils() {
    }

    public static String[] split(String command) {
        if (command == null || command.trim().isEmpty()) {
            throw new CredentialException("process_command is empty");
        }
        char[] chars = command.trim().toCharArray();
        List<String> args = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;
        // Tracks that a token has started even if it is empty, so quoted empty
        // arguments like `tool "" arg` keep their empty argv element.
        boolean hasToken = false;

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (inSingle) {
                if (c == '\'') {
                    inSingle = false;
                } else {
                    current.append(c);
                }
                continue;
            }
            if (inDouble) {
                if (c == '"') {
                    inDouble = false;
                    continue;
                }
                if (c == '\\' && i + 1 < chars.length) {
                    char next = chars[i + 1];
                    if (next == '"' || next == '\\' || next == '$' || next == '`' || next == '\n') {
                        current.append(next);
                        i++;
                        continue;
                    }
                }
                current.append(c);
                continue;
            }
            if (c == '\\') {
                if (i + 1 >= chars.length) {
                    throw new CredentialException("invalid process_command: trailing backslash");
                }
                hasToken = true;
                current.append(chars[++i]);
                continue;
            }
            if (c == '\'') {
                inSingle = true;
                hasToken = true;
                continue;
            }
            if (c == '"') {
                inDouble = true;
                hasToken = true;
                continue;
            }
            if (Character.isWhitespace(c)) {
                if (hasToken) {
                    args.add(current.toString());
                    current.setLength(0);
                    hasToken = false;
                }
                continue;
            }
            hasToken = true;
            current.append(c);
        }

        if (inSingle || inDouble) {
            throw new CredentialException("invalid process_command: unclosed quote");
        }
        if (hasToken) {
            args.add(current.toString());
        }
        if (args.isEmpty() || StringUtils.isEmpty(args.get(0))) {
            throw new CredentialException("process_command is empty");
        }
        return args.toArray(new String[0]);
    }
}
