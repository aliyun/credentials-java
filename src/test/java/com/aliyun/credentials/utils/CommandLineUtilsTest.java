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
                CommandLineUtils.split("\"C:\\Program Files\\tool\\cred.exe\" get --profile default"));
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
    public void testEscapedSpace() {
        Assert.assertArrayEquals(
                new String[]{"tool", "arg with space"},
                CommandLineUtils.split("tool arg\\ with\\ space"));
    }

    @Test
    public void testEscapedQuoteInsideDoubleQuotes() {
        Assert.assertArrayEquals(
                new String[]{"tool", "say \"hi\""},
                CommandLineUtils.split("tool \"say \\\"hi\\\"\""));
    }

    @Test
    public void testBackslashInsideDoubleQuotesLiteral() {
        // Windows path backslashes inside double quotes stay literal
        Assert.assertArrayEquals(
                new String[]{"C:\\Program Files\\tool.exe"},
                CommandLineUtils.split("\"C:\\Program Files\\tool.exe\""));
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
    public void testTrailingBackslash() {
        try {
            CommandLineUtils.split("tool\\");
            Assert.fail();
        } catch (CredentialException e) {
            Assert.assertTrue(e.getMessage().contains("trailing backslash"));
        }
    }
}
