package com.aliyun.credentials.provider;


import com.aliyun.credentials.AlibabaCloudCredentials;
import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.exception.CredentialException;
import com.aliyun.credentials.http.CompatibleUrlConnClient;
import com.aliyun.credentials.models.Config;
import com.aliyun.credentials.utils.StringUtils;

import java.net.MalformedURLException;
import java.text.ParseException;

public class EcsRamRoleCredentialProvider implements AlibabaCloudCredentialsProvider {

    private ECSMetadataServiceCredentialsFetcher fetcher;

    public EcsRamRoleCredentialProvider(String roleName) {
        if (StringUtils.isEmpty(roleName)) {
            CompatibleUrlConnClient client = new CompatibleUrlConnClient();
            roleName = new ECSMetadataServiceCredentialsFetcher("").fetchRoleName(client);
        }
        this.fetcher = new ECSMetadataServiceCredentialsFetcher(roleName);
    }


    public EcsRamRoleCredentialProvider(Configuration config) {
        if (StringUtils.isEmpty(config.getRoleName())) {
            CompatibleUrlConnClient client = new CompatibleUrlConnClient();
            String roleName = new ECSMetadataServiceCredentialsFetcher("").fetchRoleName(client);
            config.setRoleName(roleName);
        }
        this.fetcher = new ECSMetadataServiceCredentialsFetcher(config.getRoleName(), config.getConnectTimeout(), config.getReadTimeout());
    }

    public EcsRamRoleCredentialProvider(Config config) {
        if (StringUtils.isEmpty(config.roleName)) {
            CompatibleUrlConnClient client = new CompatibleUrlConnClient();
            String roleName = new ECSMetadataServiceCredentialsFetcher("").fetchRoleName(client);
            config.roleName = roleName;
        }
        this.fetcher = new ECSMetadataServiceCredentialsFetcher(config.roleName, config.connectTimeout, config.timeout);
    }

    @Override
    public AlibabaCloudCredentials getCredentials() {
        CompatibleUrlConnClient client = new CompatibleUrlConnClient();
        return fetcher.fetch(client, this);
    }

    public ECSMetadataServiceCredentialsFetcher getFetcher() {
        return fetcher;
    }

    public void setFetcher(ECSMetadataServiceCredentialsFetcher fetcher) {
        this.fetcher = fetcher;
    }
}