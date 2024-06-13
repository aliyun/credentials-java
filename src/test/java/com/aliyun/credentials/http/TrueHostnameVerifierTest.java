package com.aliyun.credentials.http;

import org.junit.Assert;
import org.junit.Test;

public class TrueHostnameVerifierTest {
    @Test
    public void trueHostnameVerifierTest(){
        TrueHostnameVerifier trueHostnameVerifier = new TrueHostnameVerifier();
        Assert.assertTrue(trueHostnameVerifier.verify(null,null));
    }
}
