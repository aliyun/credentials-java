package com.aliyun.credentials.utils;

public final class ProviderName {
    public static final String STATIC_AK = "static_ak";
    public static final String STATIC_STS = "static_sts";
    public static final String ECS_RAM_ROLE = "ecs_ram_role";
    public static final String RAM_ROLE_ARN = "ram_role_arn";
    public static final String RSA_KEY_PAIR = "rsa_key_pair";
    public static final String OIDC_ROLE_ARN = "oidc_role_arn";
    public static final String CREDENTIALS_URI = "credentials_uri";

    public static final String ENV = "env";
    public static final String SYSTEM = "system";
    public static final String PROFILE = "profile";
    public static final String CLI_PROFILE = "cli_profile";
    public static final String DEFAULT = "default";

    private ProviderName() {
    }
}
