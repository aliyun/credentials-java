package com.aliyun.credentials.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

public class ParameterHelperTest {
    @Before
    public void init() {
        new ParameterHelper();
    }

    @Test
    public void getUniqueNonce() {
        String nonce = ParameterHelper.getUniqueNonce();
        Assert.assertNotEquals(nonce, ParameterHelper.getUniqueNonce());
    }

    @Test
    public void getISO8601Time() {
        Date d2 = ParameterHelper.getUTCDate("2018-12-18T16:39:38Z");
        Assert.assertEquals("2018-12-18T16:39:38Z", ParameterHelper.getISO8601Time(d2));
    }

    @Test
    public void signString() {
        String stringToSign = "abc~!@#";
        String sk = "sk#$!~~~";
        ParameterHelper parameterHelper = new ParameterHelper();
        String signedString = parameterHelper.signString(stringToSign, sk);
        Assert.assertEquals("7eV3A584uvdgKVk8Ck8r9ukg1gE=", signedString);
    }
}