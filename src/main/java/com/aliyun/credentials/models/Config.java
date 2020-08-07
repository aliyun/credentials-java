package com.aliyun.credentials.models;

import com.aliyun.tea.NameInMap;
import com.aliyun.tea.TeaModel;

public class Config extends TeaModel {
    @NameInMap("type")
    public String type = "default";
    @NameInMap("accessKeyId")
    public String accessKeyId;
    @NameInMap("accessKeySecret")
    public String accessKeySecret;
    @NameInMap("roleArn")
    public String roleArn;
    @NameInMap("roleSessionName")
    public String roleSessionName;
    @NameInMap("privateKeyFile")
    public String privateKeyFile;
    @NameInMap("publicKeyId")
    public String publicKeyId;
    @NameInMap("roleName")
    public String roleName;
    @NameInMap("bearerToken")
    public String bearerToken;
    @NameInMap("securityToken")
    public String securityToken;
    @NameInMap("host")
    public String host;
    @NameInMap("readTimeout")
    public int timeout;
    @NameInMap("connectTimeout")
    public int connectTimeout;
    @NameInMap("proxy")
    public String proxy;
    @NameInMap("policy")
    public String policy;
    @NameInMap("roleSessionExpiration")
    public Integer roleSessionExpiration = 3600;

    public static Config build(java.util.Map<String, ?> map) throws Exception {
        Config self = new Config();
        return TeaModel.build(map, self);
    }

}
