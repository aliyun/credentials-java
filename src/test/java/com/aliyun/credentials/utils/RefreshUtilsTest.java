package com.aliyun.credentials.utils;

import com.aliyun.credentials.provider.EcsRamRoleCredentialProvider;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class RefreshUtilsTest {

    @Test
    public void getNewCredentialTest() {
        new RefreshUtils();
        EcsRamRoleCredentialProvider provider = Mockito.mock(EcsRamRoleCredentialProvider.class);
        Mockito.when(provider.getCredentials()).thenThrow(new RuntimeException("This exception is expected"));
        Assert.assertNull(RefreshUtils.getNewCredential(provider));
    }
}
