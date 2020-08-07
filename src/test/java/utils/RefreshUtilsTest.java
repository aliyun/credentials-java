package utils;

import com.aliyun.credentials.provider.EcsRamRoleCredentialProvider;
import com.aliyun.credentials.utils.RefreshUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class RefreshUtilsTest {

    @Test
    public void getNewCredentialTest() throws Exception{
        new RefreshUtils();
        EcsRamRoleCredentialProvider provider = Mockito.mock(EcsRamRoleCredentialProvider.class);
        Mockito.when(provider.getCredentials()).thenThrow(new RuntimeException("This exception is expected"));
        Assert.assertNull(RefreshUtils.getNewCredential(provider));
    }
}
