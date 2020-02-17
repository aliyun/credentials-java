package com.aliyun.credentials.models;

import com.aliyun.tea.NameInMap;
import com.aliyun.tea.TeaModel;

public class Config extends TeaModel {
    @NameInMap("type")
    public String type = "default";
    @NameInMap("access_key_id")
    public String accessKeyId;
    @NameInMap("access_key_secret")
    public String accessKeySecret;
    @NameInMap("role_arn")
    public String roleArn;
    @NameInMap("role_session_name")
    public String roleSessionName;
    @NameInMap("private_key_file")
    public String privateKeyFile;
    @NameInMap("public_key_id")
    public String publicKeyId;
    @NameInMap("role_name")
    public String roleName;
    @NameInMap("bearer_token")
    public String bearerToken;
    @NameInMap("security_token")
    public String securityToken;
    @NameInMap("host")
    public String host;
    @NameInMap("rad_time_out")
    public int timeout;
    @NameInMap("connect_timeout")
    public int connectTimeout;
    @NameInMap("proxy")
    public String proxy;

    public static Config build(java.util.Map<String, ?> map) throws Exception {
        Config self = new Config();
        return TeaModel.build(map, self);
    }

}
