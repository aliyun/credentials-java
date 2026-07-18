package com.aliyun.credentials.utils;

import com.aliyun.credentials.exception.CredentialException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Split process_command into argv with quote support so paths containing spaces
 * (e.g. Windows "C:\\Program Files\\...") can be passed as a single argument.
 * <p>
 * On Unix, escape rules follow POSIX shlex: outside quotes, '\' escapes the
 * next char; inside double quotes, '\' only escapes '"', '\', '$' and '`';
 * backslash-newline is a line continuation (both removed) outside single
 * quotes; inside single quotes, all characters are literal.
 * <p>
 * On Windows, '\' is a path separator and is treated as a literal (except
 * '\"' inside double quotes), so unquoted paths like C:\tools\cred.exe keep
 * their backslashes.
 */
public final class CommandLineUtils {
    private CommandLineUtils() {
    }

    public static String[] split(String command) {
        return split(command, File.separatorChar == '\\');
    }

    static String[] split(String command, boolean windows) {
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
                    if (windows) {
                        // On Windows only \" is an escape inside double quotes.
                        if (next == '"') {
                            current.append(next);
                            i++;
                            continue;
                        }
                    } else if (next == '\n') {
                        // Backslash-newline is a line continuation: both removed.
                        i++;
                        continue;
                    } else if (next == '"' || next == '\\' || next == '$' || next == '`') {
                        current.append(next);
                        i++;
                        continue;
                    }
                }
                current.append(c);
                continue;
            }
            if (c == '\\') {
                if (windows) {
                    // Path separator — keep literal.
                    hasToken = true;
                    current.append(c);
                    continue;
                }
                if (i + 1 >= chars.length) {
                    throw new CredentialException("invalid process_command: trailing backslash");
                }
                if (chars[i + 1] == '\n') {
                    // Backslash-newline is a line continuation: both removed.
                    i++;
                    continue;
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
