English | [简体中文](./README-CN.md)

![](https://aliyunsdk-pages.alicdn.com/icons/AlibabaCloud.svg)

# Alibaba Cloud Credentials for Java
[![codecov](https://codecov.io/gh/aliyun/credentials-java/branch/master/graph/badge.svg)](https://codecov.io/gh/aliyun/credentials-java)
[![Travis Build Status](https://travis-ci.org/aliyun/credentials-java.svg?branch=master)](https://travis-ci.org/aliyun/credentials-java)
[![Appveyor Build Status](https://ci.appveyor.com/api/projects/status/6jxpwmhyfipagtge/branch/master?svg=true)](https://ci.appveyor.com/project/aliyun/credentials-java)
[![Latest Stable Version](https://img.shields.io/maven-central/v/com.aliyun/credentials-java.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.aliyun%22%20AND%20a:%22credentials-java%22)

Alibaba Cloud Credentials for Java is a tool for Java developers to manage credentials.

This document introduces how to obtain and use Credentials for Java.

## Requirements

- The Alibaba Cloud Credentials for Java requires JDK 1.8 or later.

## Installation

```xml
<dependency>
   <groupId>com.aliyun</groupId>
   <artifactId>credentials-java</artifactId>
   <version>Use the version shown in the maven badge</version>
</dependency>
```


## Quick Examples

Before you begin, you need to sign up for an Alibaba Cloud account and retrieve your [Credentials](https://usercenter.console.aliyun.com/#/manage/ak).

### Credential Type

#### AccessKey
Setup access_key credential through [User Information Management][ak], it have full authority over the account, please keep it safe. Sometimes for security reasons, you cannot hand over a primary account AccessKey with full access to the developer of a project. You may create a sub-account [RAM Sub-account][ram] , grant its [authorization][permissions]，and use the AccessKey of RAM Sub-account.

```java
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;

public class DemoTest {
    public static void main(String[] args) throws Exception{
        Config config = new Config();
        // Which type of credential you want
        config.type = "access_key";
        // AccessKeyId of your account
        config.accessKeyId = "AccessKeyId";
        // AccessKeySecret of your account
        config.accessKeySecret = "AccessKeySecret";
        Client client = new Client(config);
    }
}
```

#### STS
Create a temporary security credential by applying Temporary Security Credentials (TSC) through the Security Token Service (STS).

```java
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;

public class DemoTest {
    public static void main(String[] args) throws Exception{
        Config config = new Config();
        // Which type of credential you want
        config.type = "sts";
        // AccessKeyId of your account
        config.accessKeyId = "AccessKeyId";
        // AccessKeySecret of your account
        config.accessKeySecret = "AccessKeySecret";
        // Temporary Security Token
        config.securityToken = "SecurityToken";
        Client client = new Client(config);
    }
}
```

#### RamRoleArn
By specifying [RAM Role][RAM Role], the credential will be able to automatically request maintenance of STS Token. If you want to limit the permissions([How to make a policy][policy]) of STS Token, you can assign value for `Policy`.

```java
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;

public class DemoTest {
    public static void main(String[] args) throws Exception{
        Config config = new Config();
        // Which type of credential you want        
        config.type = "ram_role_arn";
        // AccessKeyId of your account
        config.accessKeyId = "AccessKeyId";
        // AccessKeySecret of your account
        config.accessKeySecret = "AccessKeySecret";
        // Format: acs:ram::USER_Id:role/ROLE_NAME
        config.roleArn = "RoleArn";
        // Role Session Name
        config.roleSessionName = "RoleSessionName";
        // Not required, limit the permissions of STS Token
        config.policy = "policy";
        // Not required, limit the Valid time of STS Token
        config.roleSessionExpiration = 3600;
        Client client = new Client(config);
    }
}
```

#### OIDCRoleArn
By specifying [OIDC Role][OIDC Role], the credential will be able to automatically request maintenance of STS Token. If you want to limit the permissions([How to make a policy][policy]) of STS Token, you can assign value for `Policy`.

```java
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;

public class DemoTest {
    public static void main(String[] args) throws Exception{
        Config config = new Config();
        // Which type of credential you want        
        config.type = "oidc_role_arn";
        // `accessKeyId` is optional
        config.accessKeyId = "AccessKeyId";
        // `accessKeySecret` is optional
        config.accessKeySecret = "AccessKeySecret";
        // Format: acs:ram::USER_Id:role/ROLE_NAME
        config.roleArn = "RoleArn";
        // Format: acs:ram::USER_Id:oidc-provider/OIDC Providers 
        config.oidcProviderArn = "OIDCProviderArn";
        // Format: path
        // OIDCTokenFilePath can be replaced by setting environment variable: ALIBABA_CLOUD_OIDC_TOKEN_FILE
        config.oidcTokenFilePath = "/Users/xxx/xxx";
        // Role Session Name
        config.roleSessionName = "RoleSessionName";
        // Not required, limit the permissions of STS Token
        config.policy = "policy";
        // Not required, limit the Valid time of STS Token
        config.roleSessionExpiration = 3600;
        Client client = new Client(config);
    }
}
```

#### EcsRamRole
By specifying the role name, the credential will be able to automatically request maintenance of STS Token.

```java
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;

public class DemoTest {
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        // Which type of credential you want
        config.type = "ecs_ram_role";
        // `roleName` is optional. It will be retrieved automatically if not set. It is highly recommended to set it up to reduce requests
        config.roleName = "RoleName";
        Client client = new Client(config);
    }
}
```

#### RsaKeyPair
By specifying the public key Id and the private key file, the credential will be able to automatically request maintenance of the AccessKey before sending the request. Only Japan station is supported. 

```java
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;

public class DemoTest {
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        // Which type of credential you want
        config.type = "rsa_key_pair";
        // The file path to store the PrivateKey
        config.privateKeyFile = "PrivateKeyFile";
        // PublicKeyId of your account
        config.publicKeyId = "PublicKeyId";
        Client client = new Client(config);
    }
}
```

#### URLCredential
By specifying the url, the credential will be able to automatically request maintenance of STS Token.

```java
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;

public class DemoTest {
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        // Which type of credential you want
        config.type = "credentials_uri";
        // Format: http url. `credentialsURI` can be replaced by setting environment variable: ALIBABA_CLOUD_CREDENTIALS_URI
        config.credentialsURI = "http://xxx";
        Client client = new Client(config);
    }
}
```

#### Bearer Token
If credential is required by the Cloud Call Centre (CCC), please apply for Bearer Token maintenance by yourself.

```java
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;

public class DemoTest {
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        // Which type of credential you want
        config.type = "bearer";
        // BearerToken of your account
        config.bearerToken = "BearerToken";
        Client client = new Client(config);
    }
}
```

### Use the default credential provider chain
If you call `Client client = new Client()`, it will use provider chain to get credential for you.

The default credential provider chain looks for available credentials, with following order:

1.System Properties

Look for environment credentials in system properties. If the `alibabacloud.accessKeyId` and `alibabacloud.accessKeyIdSecret` system properties are defined and not empty, the program will use them to create default credentials.

2.Environment Credentials

Look for environment credentials in environment variable. If the `ALIBABA_CLOUD_ACCESS_KEY_ID` and `ALIBABA_CLOUD_ACCESS_KEY_SECRET` environment variables are defined and are not empty, the program will use them to create default credentials.

3.Credentials File

If there is `~/.alibabacloud/credentials default file (Windows shows C:\Users\USER_NAME\.alibabacloud\credentials)`, the program automatically creates credentials with the specified type and name. The default file is not necessarily exist, but a parse error will throw an exception. The name of configuration item is lowercase.This configuration file can be shared between different projects and between different tools. Because it is outside of the project and will not be accidentally committed to the version control. The path to the default file can be modified by defining the `ALIBABA_CLOUD_CREDENTIALS_FILE` environment variable. If not configured, use the default configuration `default`. You can also set the environment variables `ALIBABA_CLOUD_PROFILE` to use the configuration.

```ini
[default]                          # default setting
enable = true                      # Enable，Enabled by default if this option is not present
type = access_key                  # Certification type: access_key
access_key_id = foo                # Key
access_key_secret = bar            # Secret

[client1]                          # configuration that is named as `client1`
type = ecs_ram_role                # Certification type: ecs_ram_role
role_name = EcsRamRoleTest         # Role Name

[client2]                          # configuration that is named as `client2`
enable = false                     # Disable
type = ram_role_arn                # Certification type: ram_role_arn
region_id = cn-test                 
policy = test                      # optional Specify permissions
access_key_id = foo                
access_key_secret = bar            
role_arn = role_arn                
role_session_name = session_name   # optional

[client3]                          # configuration that is named as `client3`
type = rsa_key_pair                # Certification type: rsa_key_pair
public_key_id = publicKeyId        # Public Key ID
private_key_file = /your/pk.pem    # Private Key file

[client4]                          # configuration that is named as `client4`
enable = false                     # Disable
type = oidc_role_arn               # Certification type: oidc_role_arn
region_id = cn-test                 
policy = test                      # optional Specify permissions
access_key_id = foo                # optional
access_key_secret = bar            # optional
role_arn = role_arn
oidc_provider_arn = oidc_provider_arn
oidc_token_file_path = /xxx/xxx    # can be replaced by setting environment variable: ALIBABA_CLOUD_OIDC_TOKEN_FILE              
role_session_name = session_name   # optional
```


## Issues
[Opening an Issue](https://github.com/aliyun/credentials-java/issues/new), Issues not conforming to the guidelines may be closed immediately.

## Changelog
Detailed changes for each release are documented in the [release notes](./ChangeLog.txt).

## Contribution
Please make sure to read the [Contributing Guide](./.github/PULL_REQUEST_TEMPLATE.md) before making a pull request.

## References
* [Alibaba Cloud Regions & Endpoints](https://developer.aliyun.com/endpoints)
* [OpenAPI Developer Portal](https://next.api.aliyun.com/)
* [Latest Release](https://github.com/aliyun/aliyun-openapi-java-sdk)

## License
[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Copyright 2009-present Alibaba Cloud All rights reserved.
