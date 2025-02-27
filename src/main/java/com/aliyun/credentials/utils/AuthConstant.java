package com.aliyun.credentials.utils;


import com.aliyun.credentials.configure.Config;

public class AuthConstant {
    public static final String SYSTEM_ACCESSKEYID = Config.PROPERTIES_PREFIX + "accessKeyId";
    public static final String SYSTEM_ACCESSKEYSECRET = Config.PROPERTIES_PREFIX + "accessKeyIdSecret";
    public static final String SYSTEM_ACCESSKEY_SECRET = Config.PROPERTIES_PREFIX + "accessKeySecret";
    public static final String SYSTEM_SESSION_TOKEN = Config.PROPERTIES_PREFIX + "sessionToken";

    public static final String DEFAULT_CREDENTIALS_FILE_PATH = System.getProperty("user.home") +
            "/" + Config.CREDENTIAL_FILE_PATH + "/credentials.ini";
    public static final String INI_ACCESS_KEY_ID = "access_key_id";
    public static final String INI_ACCESS_KEY_IDSECRET = "access_key_secret";
    public static final String INI_TYPE = "type";
    public static final String INI_TYPE_RAM = "ecs_ram_role";
    public static final String INI_TYPE_ARN = "ram_role_arn";
    public static final String INI_TYPE_OIDC = "oidc_role_arn";
    public static final String INI_TYPE_KEY_PAIR = "rsa_key_pair";
    public static final String INI_PUBLIC_KEY_ID = "public_key_id";
    public static final String INI_PRIVATE_KEY_FILE = "private_key_file";
    public static final String INI_PRIVATE_KEY = "private_key";
    public static final String INI_ROLE_NAME = "role_name";
    public static final String INI_ROLE_SESSION_NAME = "role_session_name";
    public static final String INI_ROLE_ARN = "role_arn";
    public static final String INI_POLICY = "policy";
    public static final String INI_OIDC_PROVIDER_ARN = "oidc_provider_arn";
    public static final String INI_OIDC_TOKEN_FILE_PATH = "oidc_token_file_path";
    public static final String DEFAULT_REGION = "region_id";
    public static final String INI_ENABLE = "enable";

    public static final String ACCESS_KEY = "access_key";
    public static final String STS = "sts";
    public static final String ECS_RAM_ROLE = "ecs_ram_role";
    public static final String RAM_ROLE_ARN = "ram_role_arn";
    public static final String RSA_KEY_PAIR = "rsa_key_pair";
    public static final String BEARER = "bearer";
    public static final String OIDC_ROLE_ARN = "oidc_role_arn";
    public static final String CREDENTIALS_URI = "credentials_uri";

}
