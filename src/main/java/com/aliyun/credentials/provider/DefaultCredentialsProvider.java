package com.aliyun.credentials.provider;


import com.aliyun.credentials.AlibabaCloudCredentials;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.utils.AuthUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class DefaultCredentialsProvider implements AlibabaCloudCredentialsProvider {
    private List<AlibabaCloudCredentialsProvider> defaultProviders = new ArrayList<AlibabaCloudCredentialsProvider>();
    private static final List<AlibabaCloudCredentialsProvider> USER_CONFIGURATION_PROVIDERS =
            new Vector<AlibabaCloudCredentialsProvider>();

    public DefaultCredentialsProvider() throws CredentialException, MalformedURLException {
        defaultProviders.add(new SystemPropertiesCredentialsProvider());
        defaultProviders.add(new EnvironmentVariableCredentialsProvider());
        defaultProviders.add(new ProfileCredentialsProvider());
        String roleName = AuthUtils.getEnvironmentECSMetaData();
        if (roleName != null) {
            defaultProviders.add(new EcsRamRoleCredentialProvider(roleName));
        }
    }

    @Override
    public AlibabaCloudCredentials getCredentials() throws CredentialException, IOException, ParseException {
        AlibabaCloudCredentials credential;
        if (USER_CONFIGURATION_PROVIDERS.size() > 0) {
            for (AlibabaCloudCredentialsProvider provider : USER_CONFIGURATION_PROVIDERS) {
                credential = provider.getCredentials();
                if (null != credential) {
                    return credential;
                }
            }
        }
        for (AlibabaCloudCredentialsProvider provider : defaultProviders) {
            credential = provider.getCredentials();
            if (null != credential) {
                return credential;
            }
        }
        throw new CredentialException("not found credentials");
    }

    public static boolean addCredentialsProvider(AlibabaCloudCredentialsProvider provider) {
        return DefaultCredentialsProvider.USER_CONFIGURATION_PROVIDERS.add(provider);
    }

    public static boolean removeCredentialsProvider(AlibabaCloudCredentialsProvider provider) {
        return DefaultCredentialsProvider.USER_CONFIGURATION_PROVIDERS.remove(provider);
    }

    public static boolean containsCredentialsProvider(AlibabaCloudCredentialsProvider provider) {
        return DefaultCredentialsProvider.USER_CONFIGURATION_PROVIDERS.contains(provider);
    }

    public static void clearCredentialsProvider() {
        DefaultCredentialsProvider.USER_CONFIGURATION_PROVIDERS.clear();
    }
}
