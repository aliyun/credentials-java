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
    private static final List<AlibabaCloudCredentialsProvider> userConfigurationProviders =
            new Vector<AlibabaCloudCredentialsProvider>();

    public DefaultCredentialsProvider() throws CredentialException, MalformedURLException {
        defaultProviders.add(new SystemPropertiesCredentialsProvider());
        defaultProviders.add(new EnvironmentVariableCredentialsProvider());
        defaultProviders.add(new ProfileCredentialsProvider());
        String roleName = AuthUtils.getEnvironmentECSMetaData();
        if (roleName != null) {
            if (roleName.length() == 0) {
                throw new CredentialException("Environment variable roleName('ALIBABA_CLOUD_ECS_METADATA') cannot be empty");
            }
            defaultProviders.add(new EcsRamRoleCredentialProvider(roleName));
        }
    }

    @Override
    public AlibabaCloudCredentials getCredentials() throws CredentialException, IOException, ParseException {
        AlibabaCloudCredentials credential;
        if (userConfigurationProviders.size() > 0) {
            for (AlibabaCloudCredentialsProvider provider : userConfigurationProviders) {
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
        return DefaultCredentialsProvider.userConfigurationProviders.add(provider);
    }

    public static boolean removeCredentialsProvider(AlibabaCloudCredentialsProvider provider) {
        return DefaultCredentialsProvider.userConfigurationProviders.remove(provider);
    }

    public static boolean containsCredentialsProvider(AlibabaCloudCredentialsProvider provider) {
        return DefaultCredentialsProvider.userConfigurationProviders.contains(provider);
    }

    public static void clearCredentialsProvider() {
        DefaultCredentialsProvider.userConfigurationProviders.clear();
    }
}
