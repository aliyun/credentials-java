package com.aliyun.credentials.exception;

import org.junit.Assert;
import org.junit.Test;

public class CredentialExceptionTest {

    @Test
    public void getMessageTest(){
        CredentialException exception = new CredentialException("test");
        Assert.assertEquals("test", exception.getMessage());
    }
}
