package com.aliyun.credentials.utils;

import com.aliyun.credentials.exception.CredentialException;
import org.junit.Assert;
import org.junit.Test;

public class CommandLineUtilsTest {

    @Test
    public void testSimple() {
        Assert.assertArrayEquals(
                new String[]{"cmd", "arg1", "arg2"},
                CommandLineUtils.split("cmd arg1 arg2"));
    }

    @Test
    public void testExtraWhitespace() {
        Assert.assertArrayEquals(
                new String[]{"cmd", "arg1", "arg2"},
                CommandLineUtils.split("  cmd   arg1\targ2  "));
    }

    @Test
    public void testWindowsQuotedPath() {
        Assert.assertArrayEquals(
                new String[]{"C:\\Program Files\\tool\\cred.exe", "get", "--profile", "default"},
                CommandLineUtils.split("\"C:\\Program Files\\tool\\cred.exe\" get --profile default", true));
    }

    @Test
    public void testUnixSingleQuotedPath() {
        Assert.assertArrayEquals(
                new String[]{"/usr/local/my tools/cred", "arg"},
                CommandLineUtils.split("'/usr/local/my tools/cred' arg"));
    }

    @Test
    public void testQuotedArgument() {
        Assert.assertArrayEquals(
                new String[]{"tool", "--name", "First Last"},
                CommandLineUtils.split("tool --name \"First Last\""));
    }

    @Test
    public void testEscapedSpaceUnix() {
        Assert.assertArrayEquals(
                new String[]{"tool", "arg with space"},
                CommandLineUtils.split("tool arg\\ with\\ space", false));
    }

    @Test
    public void testEscapedQuoteInsideDoubleQuotesUnix() {
        Assert.assertArrayEquals(
                new String[]{"tool", "say \"hi\""},
                CommandLineUtils.split("tool \"say \\\"hi\\\"\"", false));
    }

    @Test
    public void testSingleQuotedKeepsBackslashesUnix() {
        // printf-style octal escapes must survive tokenizing when single quoted
        Assert.assertArrayEquals(
                new String[]{"/usr/bin/printf", "\\173\\042mode\\042\\175"},
                CommandLineUtils.split("/usr/bin/printf '\\173\\042mode\\042\\175'", false));
    }

    @Test
    public void testWindowsUnquotedPathKeepsBackslashes() {
        Assert.assertArrayEquals(
                new String[]{"C:\\tools\\cred.exe", "get"},
                CommandLineUtils.split("C:\\tools\\cred.exe get", true));
    }

    @Test
    public void testWindowsEscapedQuoteInsideDoubleQuotes() {
        Assert.assertArrayEquals(
                new String[]{"tool", "say \"hi\""},
                CommandLineUtils.split("tool \"say \\\"hi\\\"\"", true));
    }

    @Test
    public void testWindowsBackslashInsideDoubleQuotesLiteral() {
        // Windows path backslashes inside double quotes stay literal
        Assert.assertArrayEquals(
                new String[]{"C:\\Program Files\\tool.exe"},
                CommandLineUtils.split("\"C:\\Program Files\\tool.exe\"", true));
    }

    @Test
    public void testEmptyDoubleQuotedArgument() {
        Assert.assertArrayEquals(
                new String[]{"tool", "", "arg"},
                CommandLineUtils.split("tool \"\" arg"));
    }

    @Test
    public void testEmptySingleQuotedArgument() {
        Assert.assertArrayEquals(
                new String[]{"tool", "", "arg"},
                CommandLineUtils.split("tool '' arg"));
    }

    @Test
    public void testAdjacentQuotedSegmentsFormOneArgument() {
        Assert.assertArrayEquals(
                new String[]{"tool", "a bc d"},
                CommandLineUtils.split("tool \"a b\"'c d'"));
    }

    @Test(expected = CredentialException.class)
    public void testEmpty() {
        CommandLineUtils.split("   ");
    }

    @Test(expected = CredentialException.class)
    public void testEmptyQuotedOnly() {
        // "" alone yields empty argv[0]
        CommandLineUtils.split("\"\"");
    }

    @Test
    public void testUnclosedDoubleQuote() {
        try {
            CommandLineUtils.split("\"C:\\Program Files\\tool.exe");
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("unclosed quote"));
        }
    }

    @Test
    public void testTrailingBackslashUnix() {
        try {
            CommandLineUtils.split("tool\\", false);
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("trailing backslash"));
        }
    }
}
