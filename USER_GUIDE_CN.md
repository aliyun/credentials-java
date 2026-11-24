# 阿里云 Credentials Java SDK - 用户故障排查指南

## 1. 项目简介

**Alibaba Cloud Credentials for Java** 是阿里云官方提供的 Java 凭证管理工具，用于帮助开发者安全、便捷地管理和获取访问阿里云服务所需的身份凭证。

### 核心能力

- **多种凭证类型支持**：支持 AccessKey、STS Token、ECS RAM 角色、RAM 角色扮演、OIDC、RSA 密钥对、Bearer Token 等多种凭证类型
- **自动凭证刷新**：对于临时凭证（如 STS Token），SDK 会自动在过期前刷新，无需手动管理
- **凭证提供链**：支持按优先级从多个来源自动获取凭证，简化配置
- **安全性**：敏感信息不会输出到日志，支持 IMDSv2 等安全特性

### 典型应用场景

- **本地开发环境**：使用环境变量或配置文件存储 AccessKey，便于本地调试
- **ECS 实例运行**：通过实例绑定的 RAM 角色自动获取临时凭证，无需硬编码密钥
- **CI/CD 流水线**：使用 OIDC 或临时 STS Token，避免泄露长期密钥
- **容器化部署**：通过环境变量或 Credentials URI 动态获取凭证
- **多云/混合云场景**：使用 RAM 角色扮演实现跨账号或跨平台的凭证管理

---

## 2. 使用前提与环境要求

### 操作系统要求

- **支持平台**：Linux、macOS、Windows
- **特殊说明**：
  - ECS RAM 角色凭证仅在阿里云 ECS 实例内可用
  - OIDC 凭证需要文件系统支持（读取 Token 文件）

### 运行时环境

- **JDK 版本**：**JDK 1.8 或更高版本**（从 1.0.0 版本起不再支持 JDK 7）
- **构建工具**：Maven 3.x 或 Gradle（推荐 Maven 3.6+）

### Maven 依赖配置

```xml
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>credentials-java</artifactId>
    <version>1.0.3</version> <!-- 请使用最新版本 -->
</dependency>
```

### 系统依赖

- **网络访问**：
  - 使用 RAM 角色扮演、OIDC、RSA 密钥对时，需要访问 STS 服务（默认 `sts.aliyuncs.com`）
  - 使用 ECS RAM 角色时，需要访问 ECS 元数据服务（`100.100.100.200`）
  - 使用 Credentials URI 时，需要访问指定的 HTTP/HTTPS 服务

- **文件系统权限**：
  - 配置文件路径默认为 `~/.alibabacloud/credentials`（Linux/macOS）或 `C:\Users\<用户名>\.alibabacloud\credentials`（Windows）
  - OIDC Token 文件、RSA 私钥文件需要具备读取权限

### 权限要求

- **RAM 权限**：
  - 使用 RAM 角色扮演时，AccessKey 对应的用户需要拥有 `sts:AssumeRole` 权限
  - 使用 RSA 密钥对时，需要 `sts:GenerateSessionAccessKey` 权限
  - 使用 OIDC 时，需要 `sts:AssumeRoleWithOIDC` 权限

- **ECS 实例元数据访问**：
  - ECS 实例需要绑定 RAM 角色
  - 安全组规则需允许访问元数据服务（默认已放行）

### 可选环境变量

SDK 支持通过环境变量覆盖配置，常用环境变量包括：

| 环境变量名称 | 用途 | 示例值 |
|-------------|------|--------|
| `ALIBABA_CLOUD_ACCESS_KEY_ID` | AccessKey ID | `LTAI5t...` |
| `ALIBABA_CLOUD_ACCESS_KEY_SECRET` | AccessKey Secret | `xxx...` |
| `ALIBABA_CLOUD_SECURITY_TOKEN` | STS Token | `CAISxxx...` |
| `ALIBABA_CLOUD_ROLE_ARN` | RAM 角色 ARN | `acs:ram::123456:role/MyRole` |
| `ALIBABA_CLOUD_ROLE_SESSION_NAME` | 角色会话名称 | `session-name` |
| `ALIBABA_CLOUD_OIDC_PROVIDER_ARN` | OIDC 提供商 ARN | `acs:ram::123456:oidc-provider/provider` |
| `ALIBABA_CLOUD_OIDC_TOKEN_FILE` | OIDC Token 文件路径 | `/var/run/secrets/token` |
| `ALIBABA_CLOUD_CREDENTIALS_FILE` | 凭证配置文件路径 | `/path/to/credentials` |
| `ALIBABA_CLOUD_PROFILE` | 使用的配置节名称 | `default` |
| `ALIBABA_CLOUD_ECS_METADATA` | ECS 角色名称 | `MyEcsRole` |
| `ALIBABA_CLOUD_IMDSV1_DISABLED` | 禁用 IMDSv1 | `true` |
| `ALIBABA_CLOUD_ECS_METADATA_DISABLED` | 禁用 ECS 元数据 | `true` |
| `ALIBABA_CLOUD_CLI_PROFILE_DISABLED` | 禁用 CLI Profile | `true` |
| `ALIBABA_CLOUD_CREDENTIALS_URI` | 凭证服务 URI | `http://localhost:8080/credentials` |

---

## 3. 快速开始指南

### 安装步骤

#### 1. 添加 Maven 依赖

在项目的 `pom.xml` 文件中添加：

```xml
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>credentials-java</artifactId>
    <version>1.0.3</version>
</dependency>
```

#### 2. 构建项目

```bash
mvn clean install
```

### 配置说明

#### 方式一：使用环境变量（推荐用于容器化部署）

```bash
export ALIBABA_CLOUD_ACCESS_KEY_ID="your-access-key-id"
export ALIBABA_CLOUD_ACCESS_KEY_SECRET="your-access-key-secret"
```

#### 方式二：使用配置文件（推荐用于本地开发）

创建文件 `~/.alibabacloud/credentials`：

```ini
[default]
enable = true
type = access_key
access_key_id = your-access-key-id
access_key_secret = your-access-key-secret
```

#### 方式三：在代码中显式配置

```java
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;

public class Example {
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        config.setType("access_key");
        config.setAccessKeyId("your-access-key-id");
        config.setAccessKeySecret("your-access-key-secret");
        
        Client client = new Client(config);
        System.out.println("AccessKeyId: " + client.getCredential().getAccessKeyId());
    }
}
```

### 启动与验证

#### 使用默认凭证提供链（零配置）

```java
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.CredentialModel;

public class QuickStart {
    public static void main(String[] args) {
        try {
            // 自动按优先级查找凭证
            Client client = new Client();
            CredentialModel credential = client.getCredential();
            
            System.out.println("凭证类型: " + credential.getType());
            System.out.println("AccessKeyId: " + credential.getAccessKeyId());
            System.out.println("提供者: " + credential.getProviderName());
            
        } catch (Exception e) {
            System.err.println("获取凭证失败: " + e.getMessage());
        }
    }
}
```

#### 预期输出示例

```
凭证类型: access_key
AccessKeyId: LTAI5t***
提供者: default/env
```

---

## 4. 常见问题解答（FAQ）

### 4.1 凭证链相关问题

#### Q1: 报错 "Unable to load credentials from any of the providers in the chain"

**问题现象**：
```
com.aliyun.credentials.exception.CredentialException: Unable to load credentials from any of the providers in the chain: [SystemPropertiesCredentialsProvider: ..., EnvironmentVariableCredentialsProvider: ..., ...]
```

**根本原因**：
使用默认凭证提供链时（即 `new Client()` 不传参数），SDK 会按以下顺序尝试获取凭证：
1. 系统属性（System Properties）
2. 环境变量
3. OIDC 凭证（如果环境变量配置了相关参数）
4. Aliyun CLI 配置文件
5. `~/.alibabacloud/credentials` 配置文件
6. ECS 实例元数据服务
7. Credentials URI（如果配置了环境变量）

当所有来源都无法获取到有效凭证时，会抛出此异常。

**解决方案**：

1. **检查环境变量是否正确设置**：
   ```bash
   echo $ALIBABA_CLOUD_ACCESS_KEY_ID
   echo $ALIBABA_CLOUD_ACCESS_KEY_SECRET
   ```

2. **检查配置文件是否存在且格式正确**：
   ```bash
   cat ~/.alibabacloud/credentials
   ```
   
   确保至少有一个启用的配置节：
   ```ini
   [default]
   enable = true
   type = access_key
   access_key_id = your-key-id
   access_key_secret = your-key-secret
   ```

3. **显式配置凭证，不依赖凭证链**：
   ```java
   Config config = new Config();
   config.setType("access_key");
   config.setAccessKeyId("your-key-id");
   config.setAccessKeySecret("your-key-secret");
   Client client = new Client(config);
   ```

4. **查看详细错误信息**（错误消息中会列出每个提供者失败的原因）：
   - 仔细阅读每个 Provider 的失败原因
   - 常见子错误包括："The specified credentials file is empty"、"Client is not open in the specified credentials file" 等

---

#### Q2: 配置文件存在但仍报 "Client is not open in the specified credentials file"

**问题现象**：
配置文件 `~/.alibabacloud/credentials` 已创建，但仍提示无法找到配置。

**根本原因**：
1. 配置节的 `enable` 字段未设置为 `true`（默认为 `false`）
2. 使用了非 `default` 的配置节名称，但未通过 `ALIBABA_CLOUD_PROFILE` 环境变量指定
3. 配置文件格式错误（如缺少必要字段）

**解决方案**：

1. **确保启用配置节**：
   ```ini
   [default]
   enable = true  # 必须显式设置为 true
   type = access_key
   access_key_id = foo
   access_key_secret = bar
   ```

2. **使用非 default 配置节时，设置环境变量**：
   ```bash
   export ALIBABA_CLOUD_PROFILE=my-profile
   ```
   
   对应配置文件：
   ```ini
   [my-profile]
   enable = true
   type = access_key
   ...
   ```

3. **检查配置文件权限**（Linux/macOS）：
   ```bash
   ls -l ~/.alibabacloud/credentials
   # 确保当前用户有读权限
   chmod 600 ~/.alibabacloud/credentials
   ```

---

#### Q3: 报错 "The specified credentials file is empty"

**问题现象**：
```
CredentialException: The specified credentials file is empty.
```

**根本原因**：
通过 `ALIBABA_CLOUD_CREDENTIALS_FILE` 环境变量指定的配置文件路径为空字符串（而非 `null`）。

**解决方案**：

1. **检查环境变量**：
   ```bash
   echo "[$ALIBABA_CLOUD_CREDENTIALS_FILE]"
   # 如果输出为 []，说明是空字符串
   ```

2. **取消设置或设置正确路径**：
   ```bash
   unset ALIBABA_CLOUD_CREDENTIALS_FILE
   # 或者
   export ALIBABA_CLOUD_CREDENTIALS_FILE=/path/to/credentials
   ```

---

### 4.2 AccessKey 凭证问题

#### Q4: 报错 "AccessKeyId must not be null" 或 "AccessKeySecret must not be null"

**问题现象**：
```java
java.lang.IllegalArgumentException: AccessKeyId must not be null.
```

**根本原因**：
显式配置 `access_key` 或 `sts` 类型凭证时，未提供必需的 `accessKeyId` 或 `accessKeySecret` 字段。

**解决方案**：

1. **检查代码配置**：
   ```java
   Config config = new Config();
   config.setType("access_key");
   config.setAccessKeyId("LTAI5t..."); // 不能为 null 或空字符串
   config.setAccessKeySecret("xxx...");  // 不能为 null 或空字符串
   ```

2. **检查配置文件**：
   ```ini
   [default]
   enable = true
   type = access_key
   access_key_id = your-key-id      # 必填
   access_key_secret = your-secret  # 必填
   ```

3. **检查环境变量**：
   ```bash
   # 确保两个环境变量都已设置且非空
   [ -z "$ALIBABA_CLOUD_ACCESS_KEY_ID" ] && echo "AccessKeyId is empty!"
   [ -z "$ALIBABA_CLOUD_ACCESS_KEY_SECRET" ] && echo "AccessKeySecret is empty!"
   ```

---

#### Q5: 配置文件中 AccessKey 提示 "The configured access_key_id or access_key_secret is empty"

**问题现象**：
配置文件中明明设置了 `access_key_id` 和 `access_key_secret`，但仍提示为空。

**根本原因**：
配置文件中字段名拼写错误，正确的字段名是 `access_key_id` 和 `access_key_secret`（注意下划线），但代码内部解析时可能因为历史原因使用了不同的常量名。

**解决方案**：

1. **严格按照文档使用字段名**：
   ```ini
   [default]
   enable = true
   type = access_key
   access_key_id = foo       # 注意是 access_key_id，不是 accessKeyId
   access_key_secret = bar   # 注意是 access_key_secret
   ```

2. **避免字段名中有多余空格或特殊字符**：
   ```ini
   # 错误示例
   access_key_id  = foo  # 等号前后有多余空格可能导致解析失败
   
   # 正确示例
   access_key_id = foo
   ```

---

### 4.3 STS 临时凭证问题

#### Q6: 报错 "SecurityToken must not be null"

**问题现象**：
```
IllegalArgumentException: SecurityToken must not be null.
```

**根本原因**：
使用 `sts` 类型凭证时，`securityToken` 是必填字段，但未提供或为空。

**解决方案**：

1. **确保三个字段都提供**：
   ```java
   Config config = new Config();
   config.setType("sts");
   config.setAccessKeyId("STS.xxx");
   config.setAccessKeySecret("xxx");
   config.setSecurityToken("CAISxxx"); // 必填
   ```

2. **从 STS 服务正确获取 Token**：
   ```bash
   # 示例：通过 AssumeRole 获取临时凭证
   aliyun sts AssumeRole --RoleArn acs:ram::xxx:role/xxx --RoleSessionName test
   ```

3. **注意 Token 有效期**（默认 1 小时）：
   - STS Token 过期后会导致认证失败
   - 使用 RAM 角色扮演时，SDK 会自动刷新，无需手动管理

---

### 4.4 ECS RAM 角色问题

#### Q7: 报错 "Failed to connect ECS Metadata Service"

**问题现象**：
```
CredentialException: Failed to connect ECS Metadata Service: Connection refused
```

**根本原因**：
1. **非 ECS 环境**：在本地开发环境或非阿里云 ECS 实例上运行代码
2. **网络不通**：元数据服务地址 `100.100.100.200` 无法访问
3. **超时设置过短**：默认连接超时为 1 秒，读取超时为 1 秒

**解决方案**：

1. **确认运行环境是 ECS 实例**：
   ```bash
   curl http://100.100.100.200/latest/meta-data/
   # 应返回元数据内容，如果失败说明非 ECS 环境
   ```

2. **本地开发时禁用 ECS 元数据提供者**：
   ```bash
   export ALIBABA_CLOUD_ECS_METADATA_DISABLED=true
   ```
   
   或在代码中：
   ```java
   AuthUtils.disableECSMetaData(true);
   ```

3. **增加超时时间**（如果网络延迟较高）：
   ```java
   Config config = new Config();
   config.setType("ecs_ram_role");
   config.setConnectTimeout(5000); // 5 秒
   config.setTimeout(5000);        // 5 秒
   ```

---

#### Q8: 报错 "The role name was not found in the instance"

**问题现象**：
```
CredentialException: The role name was not found in the instance.
```

**根本原因**：
1. ECS 实例未绑定 RAM 角色
2. 指定的角色名称不存在或拼写错误

**解决方案**：

1. **检查实例是否绑定了 RAM 角色**：
   ```bash
   # 在 ECS 实例内执行
   curl http://100.100.100.200/latest/meta-data/ram/security-credentials/
   # 应返回角色名称，如果返回空或 404，说明未绑定角色
   ```

2. **在阿里云控制台为实例绑定 RAM 角色**：
   - 进入 ECS 控制台 -> 实例列表
   - 选择目标实例 -> 更多 -> 实例设置 -> 授予/收回 RAM 角色
   - 选择已创建的 RAM 角色并授予

3. **代码中不指定角色名（推荐）**：
   ```java
   Config config = new Config();
   config.setType("ecs_ram_role");
   // 不设置 roleName，SDK 会自动从元数据服务获取
   ```

4. **如果必须指定角色名，确保拼写正确**：
   ```java
   config.setRoleName("MyEcsRole"); // 与控制台中的角色名一致
   ```

---

#### Q9: 报错 "IMDS credentials is disabled"

**问题现象**：
```
CredentialException: IMDS credentials is disabled.
```

**根本原因**：
通过环境变量 `ALIBABA_CLOUD_ECS_METADATA_DISABLED=true` 禁用了 ECS 元数据服务，但代码中仍尝试使用 ECS RAM 角色凭证。

**解决方案**：

1. **取消禁用设置**：
   ```bash
   unset ALIBABA_CLOUD_ECS_METADATA_DISABLED
   ```

2. **或在代码中覆盖**：
   ```java
   AuthUtils.disableECSMetaData(false);
   ```

3. **使用其他凭证类型**（如果确实需要禁用 IMDS）：
   ```java
   Config config = new Config();
   config.setType("access_key"); // 改用 AccessKey
   config.setAccessKeyId("...");
   config.setAccessKeySecret("...");
   ```

---

#### Q10: ECS RAM 角色凭证刷新失败

**问题现象**：
日志中出现 "Failed when checking or refreshing credentials asynchronously"，但程序仍能正常运行一段时间。

**根本原因**：
SDK 默认启用异步凭证刷新机制（每分钟检查一次），网络抖动或元数据服务临时不可用会导致刷新失败，但旧凭证在有效期内仍可使用。

**解决方案**：

1. **这是警告而非错误**，如果偶尔出现可忽略
2. **检查网络稳定性**：
   ```bash
   ping -c 10 100.100.100.200
   ```

3. **增加超时时间**（如果网络延迟高）：
   ```java
   config.setConnectTimeout(3000);
   config.setTimeout(3000);
   ```

4. **禁用异步刷新**（不推荐）：
   ```java
   EcsRamRoleCredentialProvider provider = EcsRamRoleCredentialProvider.builder()
       .roleName("MyRole")
       .asyncCredentialUpdateEnabled(false) // 禁用异步刷新
       .build();
   Client client = new Client(provider);
   ```

---

### 4.5 RAM 角色扮演（RamRoleArn）问题

#### Q11: 报错 "The configured role_session_name or role_arn is empty"

**问题现象**：
```
CredentialException: The configured role_session_name or role_arn is empty.
```

**根本原因**：
使用 `ram_role_arn` 类型凭证时，`roleArn` 和 `roleSessionName` 是必填字段。

**解决方案**：

1. **显式配置所有必需字段**：
   ```java
   Config config = new Config();
   config.setType("ram_role_arn");
   config.setAccessKeyId("...");
   config.setAccessKeySecret("...");
   config.setRoleArn("acs:ram::123456789012:role/MyRole"); // 必填
   config.setRoleSessionName("mySession");                  // 必填
   ```

2. **配置文件格式**：
   ```ini
   [default]
   enable = true
   type = ram_role_arn
   access_key_id = foo
   access_key_secret = bar
   role_arn = acs:ram::123456789012:role/MyRole
   role_session_name = mySession
   ```

3. **使用环境变量**：
   ```bash
   export ALIBABA_CLOUD_ROLE_ARN=acs:ram::123456789012:role/MyRole
   export ALIBABA_CLOUD_ROLE_SESSION_NAME=mySession
   ```

---

#### Q12: 报错 "InvalidParameter: The parameter RoleSessionName is wrongly formed"

**问题现象**：
调用 STS AssumeRole 接口时返回参数格式错误。

**根本原因**：
`roleSessionName` 有格式要求：
- 长度：2-64 个字符
- 允许字符：字母、数字、`.`、`@`、`-`、`_`
- 不能包含特殊字符或空格

**解决方案**：

1. **使用符合规范的会话名称**：
   ```java
   config.setRoleSessionName("my-session-2024"); // 正确
   // config.setRoleSessionName("我的会话");      // 错误：包含中文
   // config.setRoleSessionName("a");            // 错误：长度不足
   ```

2. **不设置时会自动生成**（推荐）：
   ```java
   // SDK 会自动生成形如 "credentials-java-1702345678901" 的会话名
   Config config = new Config();
   config.setType("ram_role_arn");
   config.setRoleArn("...");
   // 不设置 roleSessionName
   ```

---

#### Q13: 报错 "You are not authorized to do this action: sts:AssumeRole"

**问题现象**：
```
SDK.ServerError: You are not authorized to do this action. You should be authorized by RAM.
```

**根本原因**：
1. 用于调用 AssumeRole 的 AccessKey 对应用户无权限
2. RAM 角色的信任策略未授权当前用户
3. 角色 ARN 填写错误或角色不存在

**解决方案**：

1. **授予 RAM 用户 AssumeRole 权限**：
   
   在 RAM 控制台为用户添加自定义策略：
   ```json
   {
     "Version": "1",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": "sts:AssumeRole",
         "Resource": "acs:ram::123456789012:role/MyRole"
       }
     ]
   }
   ```

2. **修改 RAM 角色的信任策略**：
   
   在 RAM 控制台 -> 角色管理 -> 信任策略管理，确保包含：
   ```json
   {
     "Statement": [
       {
         "Action": "sts:AssumeRole",
         "Effect": "Allow",
         "Principal": {
           "RAM": [
             "acs:ram::123456789012:user/MyUser"
           ]
         }
       }
     ],
     "Version": "1"
   }
   ```

3. **检查角色 ARN 格式**：
   ```
   正确格式：acs:ram::123456789012:role/MyRole
   # 其中 123456789012 是账号 ID，MyRole 是角色名
   ```

---

### 4.6 OIDC 凭证问题

#### Q14: 报错 "OIDCTokenFilePath xxx is not exists" 或 "cannot be read"

**问题现象**：
```
CredentialException: OIDCTokenFilePath /var/run/secrets/token is not exists.
```

**根本原因**：
1. OIDC Token 文件路径不存在
2. 文件存在但当前进程无读取权限

**解决方案**：

1. **检查文件是否存在**：
   ```bash
   ls -l /var/run/secrets/token
   ```

2. **确保文件可读**：
   ```bash
   chmod 644 /var/run/secrets/token
   # 或者只给特定用户权限
   chown myuser:mygroup /var/run/secrets/token
   chmod 600 /var/run/secrets/token
   ```

3. **Kubernetes 环境中确保 Volume 挂载正确**：
   ```yaml
   volumes:
     - name: oidc-token
       projected:
         sources:
         - serviceAccountToken:
             path: token
             expirationSeconds: 7200
             audience: "sts.aliyuncs.com"
   volumeMounts:
     - name: oidc-token
       mountPath: /var/run/secrets/tokens
       readOnly: true
   ```

4. **检查路径配置**：
   ```java
   config.setOidcTokenFilePath("/var/run/secrets/tokens/token"); // 确保路径正确
   ```

---

#### Q15: 报错 "roleArn does not exist and env ALIBABA_CLOUD_ROLE_ARN is null"

**问题现象**：
使用 OIDC 凭证时，未提供 `roleArn` 且环境变量也未设置。

**根本原因**：
OIDC 凭证需要三个必填参数：`roleArn`、`oidcProviderArn`、`oidcTokenFilePath`。如果代码中未提供，SDK 会尝试从环境变量读取，若都不存在则报错。

**解决方案**：

1. **在代码中完整配置**：
   ```java
   Config config = new Config();
   config.setType("oidc_role_arn");
   config.setRoleArn("acs:ram::123456:role/MyRole");
   config.setOidcProviderArn("acs:ram::123456:oidc-provider/MyProvider");
   config.setOidcTokenFilePath("/var/run/secrets/token");
   config.setRoleSessionName("mySession");
   ```

2. **使用环境变量（推荐用于 Kubernetes）**：
   ```bash
   export ALIBABA_CLOUD_ROLE_ARN=acs:ram::123456:role/MyRole
   export ALIBABA_CLOUD_OIDC_PROVIDER_ARN=acs:ram::123456:oidc-provider/MyProvider
   export ALIBABA_CLOUD_OIDC_TOKEN_FILE=/var/run/secrets/token
   ```
   
   然后代码中可以使用默认凭证链：
   ```java
   Client client = new Client(); // 自动从环境变量读取 OIDC 配置
   ```

---

### 4.7 配置文件格式问题

#### Q16: 配置文件解析失败或无法读取

**问题现象**：
配置文件存在但 SDK 无法正确解析，或抛出 "Unable to open credentials file" 异常。

**根本原因**：
1. 文件不存在或路径错误
2. 文件格式错误（INI 格式要求严格）
3. 文件编码问题（应使用 UTF-8）

**解决方案**：

1. **检查文件路径**：
   ```bash
   # Linux/macOS
   ls -la ~/.alibabacloud/credentials
   
   # Windows
   dir C:\Users\<用户名>\.alibabacloud\credentials
   ```

2. **确保 INI 格式正确**：
   ```ini
   # 正确示例
   [default]
   enable = true
   type = access_key
   access_key_id = foo
   access_key_secret = bar
   
   # 常见错误
   [default  # 缺少右括号
   enable = true
   type=access_key  # 等号两边最好有空格
   access_key_id =   # 值为空
   ```

3. **检查文件编码**：
   ```bash
   file -i ~/.alibabacloud/credentials
   # 应显示 charset=utf-8
   ```

4. **使用绝对路径**（避免相对路径问题）：
   ```bash
   export ALIBABA_CLOUD_CREDENTIALS_FILE=/home/user/.alibabacloud/credentials
   ```

---

#### Q17: 多个配置节时无法切换

**问题现象**：
配置文件中定义了多个配置节（如 `[default]`、`[prod]`、`[dev]`），但始终使用 `[default]`。

**根本原因**：
未通过 `ALIBABA_CLOUD_PROFILE` 环境变量指定要使用的配置节名称。

**解决方案**：

1. **设置环境变量**：
   ```bash
   export ALIBABA_CLOUD_PROFILE=prod
   ```

2. **或在代码中设置**：
   ```java
   AuthUtils.setClientType("prod");
   ```

3. **配置文件示例**：
   ```ini
   [default]
   enable = true
   type = access_key
   access_key_id = dev-key
   access_key_secret = dev-secret
   
   [prod]
   enable = true
   type = ecs_ram_role
   role_name = ProdRole
   ```

---

### 4.8 网络与超时问题

#### Q18: 报错 "Read timed out" 或 "Connect timed out"

**问题现象**：
```
java.net.SocketTimeoutException: Read timed out
```

**根本原因**：
1. 网络延迟过高，超过默认超时时间（连接 10 秒，读取 5 秒）
2. 目标服务（STS、元数据服务）不可达
3. 防火墙或安全组阻止访问

**解决方案**：

1. **增加超时时间**：
   ```java
   Config config = new Config();
   config.setType("ram_role_arn");
   config.setConnectTimeout(30000); // 30 秒
   config.setTimeout(30000);        // 30 秒
   ```

2. **检查网络连通性**：
   ```bash
   # 测试 STS 服务
   ping sts.aliyuncs.com
   curl -v https://sts.aliyuncs.com
   
   # 测试 ECS 元数据服务
   curl -v http://100.100.100.200/latest/meta-data/
   ```

3. **检查代理设置**（如果使用代理）：
   ```bash
   echo $HTTP_PROXY
   echo $HTTPS_PROXY
   ```

4. **配置 VPC 内网终端节点**（提高访问速度）：
   ```bash
   export ALIBABA_CLOUD_VPC_ENDPOINT_ENABLED=true
   ```

---

#### Q19: 无法访问 STS 服务

**问题现象**：
使用 RAM 角色扮演、OIDC 或 RSA 密钥对时，报 "Connection refused" 或 "UnknownHostException"。

**根本原因**：
1. 无外网访问权限（ECS 实例未绑定公网 IP 或 NAT 网关）
2. 域名解析失败
3. 自定义 STS Endpoint 配置错误

**解决方案**：

1. **确保可以访问公网**：
   ```bash
   ping sts.aliyuncs.com
   # 如果失败，需要配置公网访问
   ```

2. **使用 VPC 内网终端节点**（推荐）：
   ```bash
   export ALIBABA_CLOUD_VPC_ENDPOINT_ENABLED=true
   ```
   
   或在代码中：
   ```java
   AuthUtils.enableVpcEndpoint(true);
   ```

3. **自定义 STS Endpoint**（特殊场景）：
   ```java
   config.setSTSEndpoint("sts-vpc.cn-hangzhou.aliyuncs.com");
   ```

4. **检查 DNS 解析**：
   ```bash
   nslookup sts.aliyuncs.com
   ```

---

### 4.9 凭证类型与配置不匹配问题

#### Q20: 报错 "invalid type option, support: access_key, sts, ecs_ram_role, ram_role_arn, rsa_key_pair"

**问题现象**：
```
CredentialException: invalid type option, support: access_key, sts, ecs_ram_role, ram_role_arn, rsa_key_pair
```

**根本原因**：
配置的凭证类型（`type` 字段）不在支持的类型列表中，或拼写错误。

**解决方案**：

1. **使用正确的类型名称**：
   - `access_key`：长期 AccessKey
   - `sts`：临时 STS Token
   - `ecs_ram_role`：ECS RAM 角色
   - `ram_role_arn`：RAM 角色扮演
   - `oidc_role_arn`：OIDC 凭证
   - `rsa_key_pair`：RSA 密钥对
   - `bearer`：Bearer Token
   - `credentials_uri`：Credentials URI

2. **检查拼写**：
   ```java
   // 错误
   config.setType("accesskey");     // 应为 access_key
   config.setType("ram_role");      // 应为 ecs_ram_role 或 ram_role_arn
   
   // 正确
   config.setType("access_key");
   ```

---

#### Q21: 使用错误的凭证类型导致字段缺失

**问题现象**：
例如使用 `ecs_ram_role` 类型时设置了 `accessKeyId` 和 `accessKeySecret`，但凭证无效。

**根本原因**：
不同凭证类型所需的配置字段不同，混用会导致必需字段缺失。

**解决方案**：

**各类型所需字段对照表**：

| 类型 | 必需字段 | 可选字段 |
|-----|---------|---------|
| `access_key` | `accessKeyId`, `accessKeySecret` | - |
| `sts` | `accessKeyId`, `accessKeySecret`, `securityToken` | - |
| `ecs_ram_role` | - | `roleName`, `disableIMDSv1` |
| `ram_role_arn` | `accessKeyId`, `accessKeySecret`, `roleArn`, `roleSessionName` | `policy`, `externalId`, `roleSessionExpiration` |
| `oidc_role_arn` | `roleArn`, `oidcProviderArn`, `oidcTokenFilePath` | `roleSessionName`, `policy`, `roleSessionExpiration` |
| `rsa_key_pair` | `publicKeyId`, `privateKeyFile` | `roleSessionExpiration` |
| `bearer` | `bearerToken` | - |
| `credentials_uri` | `credentialsUri` | - |

**示例**：
```java
// 错误：ECS RAM 角色不需要 AccessKey
Config config = new Config();
config.setType("ecs_ram_role");
config.setAccessKeyId("xxx"); // 多余
config.setAccessKeySecret("xxx"); // 多余

// 正确
Config config = new Config();
config.setType("ecs_ram_role");
config.setRoleName("MyRole"); // 可选
```

---

### 4.10 凭证安全与最佳实践问题

#### Q22: 如何避免 AccessKey 泄露？

**问题现象**：
担心硬编码在代码中的 AccessKey 被泄露到版本控制系统或日志中。

**最佳实践**：

1. **永远不要硬编码凭证**：
   ```java
   // ❌ 错误做法
   config.setAccessKeyId("LTAI5t...");
   config.setAccessKeySecret("xxxxx");
   
   // ✅ 正确做法：使用环境变量
   String keyId = System.getenv("ALIBABA_CLOUD_ACCESS_KEY_ID");
   config.setAccessKeyId(keyId);
   ```

2. **生产环境优先使用 RAM 角色**：
   - ECS 上部署：使用 `ecs_ram_role`
   - 容器化部署：使用 `oidc_role_arn`
   - 临时访问：使用 `ram_role_arn` + STS

3. **使用配置文件时注意权限**：
   ```bash
   chmod 600 ~/.alibabacloud/credentials
   # 确保只有当前用户可读写
   ```

4. **将配置文件添加到 .gitignore**：
   ```
   .alibabacloud/
   *.credentials
   ```

5. **启用 RAM 最小权限原则**：
   - 为不同应用创建不同的 RAM 用户
   - 仅授予必需的权限
   - 定期轮换 AccessKey

---

#### Q23: 如何在日志中隐藏敏感信息？

**问题现象**：
担心 AccessKey、Token 等敏感信息出现在日志或错误堆栈中。

**SDK 保护措施**：

SDK 已内置敏感信息脱敏机制：
- `CredentialModel.toString()` 方法会自动隐藏 `accessKeySecret`、`securityToken`、`bearerToken` 等敏感字段
- 输出格式：`CredentialModel{accessKeyId='LTAI5t...', type='access_key', ...}` （不显示完整密钥）

**额外建议**：

1. **避免打印完整凭证对象**：
   ```java
   // ❌ 避免
   System.out.println("Full credential: " + credential);
   logger.info("Credential: {}", credential);
   
   // ✅ 推荐
   logger.info("Using credential type: {}", credential.getType());
   logger.info("Provider: {}", credential.getProviderName());
   ```

2. **配置日志框架脱敏规则**（以 Logback 为例）：
   ```xml
   <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
       <layout class="com.example.SensitiveDataMaskingLayout"/>
   </encoder>
   ```

---

#### Q24: 凭证过期后如何自动刷新？

**问题现象**：
使用 STS 临时凭证时，担心过期后需要重启应用。

**SDK 自动刷新机制**：

SDK 会自动管理临时凭证的刷新，无需手动干预：

1. **RAM 角色扮演（RamRoleArn）**：
   - SDK 会在凭证过期前 10 分钟自动调用 AssumeRole 刷新
   - 刷新失败时会使用旧凭证直到完全过期
   - 支持异步刷新（不阻塞业务请求）

2. **ECS RAM 角色**：
   - 默认每分钟异步检查一次凭证状态
   - 在凭证过期前 15 分钟触发刷新
   - 刷新失败会在日志中记录警告但不影响业务

3. **OIDC 凭证**：
   - 每次刷新时重新读取 Token 文件
   - 适用于 Kubernetes ServiceAccount Token 自动轮换场景

**手动控制刷新行为**：

```java
// 禁用异步刷新（不推荐）
EcsRamRoleCredentialProvider provider = EcsRamRoleCredentialProvider.builder()
    .asyncCredentialUpdateEnabled(false)
    .build();

// 自定义刷新策略
SessionCredentialsProvider provider = RamRoleArnCredentialProvider.builder()
    .asyncCredentialUpdateEnabled(true)
    .prefetchStrategy(PrefetchStrategy.builder()
        .refreshBeforeExpirationMinutes(10) // 提前 10 分钟刷新
        .build())
    .build();
```

---

### 4.11 调试与故障排查技巧

#### Q25: 如何开启调试日志以定位问题？

**解决方案**：

1. **开启 SDK 日志**：
   
   SDK 使用 SLF4J 日志框架，配置示例（Logback）：
   ```xml
   <!-- logback.xml -->
   <configuration>
       <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
           <encoder>
               <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
           </encoder>
       </appender>
       
       <logger name="com.aliyun.credentials" level="DEBUG"/>
       <logger name="com.aliyun.tea.logging" level="DEBUG"/>
       
       <root level="INFO">
           <appender-ref ref="STDOUT"/>
       </root>
   </configuration>
   ```

2. **查看凭证提供者链执行过程**：
   
   启用 DEBUG 日志后，会输出类似：
   ```
   DEBUG com.aliyun.credentials.provider.DefaultCredentialsProvider - Trying SystemPropertiesCredentialsProvider...
   DEBUG com.aliyun.credentials.provider.DefaultCredentialsProvider - SystemPropertiesCredentialsProvider failed: ...
   DEBUG com.aliyun.credentials.provider.DefaultCredentialsProvider - Trying EnvironmentVariableCredentialsProvider...
   INFO  com.aliyun.credentials.provider.DefaultCredentialsProvider - Credentials loaded from env
   ```

3. **捕获并打印详细异常信息**：
   ```java
   try {
       Client client = new Client();
       CredentialModel credential = client.getCredential();
   } catch (CredentialException e) {
       System.err.println("错误类型: " + e.getClass().getName());
       System.err.println("错误消息: " + e.getMessage());
       e.printStackTrace();
       
       // 查看完整异常链
       Throwable cause = e.getCause();
       while (cause != null) {
           System.err.println("原因: " + cause.getMessage());
           cause = cause.getCause();
       }
   }
   ```

---

#### Q26: 如何验证当前使用的凭证来源？

**解决方案**：

```java
Client client = new Client();
CredentialModel credential = client.getCredential();

System.out.println("凭证类型: " + credential.getType());
System.out.println("提供者名称: " + credential.getProviderName());
System.out.println("AccessKeyId: " + credential.getAccessKeyId());

// 提供者名称示例：
// - "default/env" - 来自环境变量
// - "default/profile/ram_role_arn" - 来自配置文件的 RAM 角色扮演
// - "default/ecs_ram_role" - 来自 ECS 元数据服务
// - "static/ak" - 来自代码显式配置
```

---

#### Q27: 如何在 CI/CD 环境中安全使用凭证？

**最佳实践**：

1. **使用 OIDC（推荐）**：
   
   GitHub Actions 示例：
   ```yaml
   - name: Configure Aliyun Credentials
     run: |
       export ALIBABA_CLOUD_ROLE_ARN="acs:ram::123456:role/GitHubActions"
       export ALIBABA_CLOUD_OIDC_PROVIDER_ARN="acs:ram::123456:oidc-provider/github"
       export ALIBABA_CLOUD_OIDC_TOKEN_FILE=$GITHUB_TOKEN_PATH
   ```

2. **使用加密的环境变量**：
   
   GitLab CI 示例：
   ```yaml
   variables:
     ALIBABA_CLOUD_ACCESS_KEY_ID: $AK_ID  # 在 GitLab 中配置为保护变量
     ALIBABA_CLOUD_ACCESS_KEY_SECRET: $AK_SECRET
   ```

3. **使用临时 STS Token**：
   ```bash
   # 在 CI 脚本中获取临时凭证
   TOKEN_RESPONSE=$(aliyun sts AssumeRole --RoleArn xxx --RoleSessionName ci-job)
   export ALIBABA_CLOUD_ACCESS_KEY_ID=$(echo $TOKEN_RESPONSE | jq -r '.Credentials.AccessKeyId')
   export ALIBABA_CLOUD_ACCESS_KEY_SECRET=$(echo $TOKEN_RESPONSE | jq -r '.Credentials.AccessKeySecret')
   export ALIBABA_CLOUD_SECURITY_TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.Credentials.SecurityToken')
   ```

---

### 4.12 版本升级与兼容性问题

#### Q28: 从 0.x 升级到 1.x 后出现兼容性问题

**问题现象**：
升级到 1.0.0+ 版本后，原有代码报错或凭证获取失败。

**主要变更**：

1. **JDK 7 不再支持**：
   - 1.0.0+ 要求 JDK 1.8+
   - 解决方案：升级 JDK 或使用 0.3.x 版本

2. **部分方法已弃用**：
   ```java
   // 旧版本（已弃用）
   client.getAccessKeyId();
   client.getAccessKeySecret();
   
   // 新版本（推荐）
   CredentialModel credential = client.getCredential();
   credential.getAccessKeyId();
   credential.getAccessKeySecret();
   ```

3. **凭证刷新机制优化**：
   - 默认启用异步刷新和缓存
   - 若需旧行为，可显式配置：
     ```java
     provider = Provider.builder()
         .asyncCredentialUpdateEnabled(false)
         .build();
     ```

**迁移建议**：

1. **检查 JDK 版本**：
   ```bash
   java -version  # 确保 >= 1.8
   ```

2. **更新 Maven 版本号**：
   ```xml
   <dependency>
       <groupId>com.aliyun</groupId>
       <artifactId>credentials-java</artifactId>
       <version>1.0.3</version>
   </dependency>
   ```

3. **替换弃用方法**：
   使用 IDE 的"查找弃用 API"功能批量替换

---

## 5. 总结与建议

### 推荐配置优先级

1. **生产环境（云上部署）**：
   - ECS：`ecs_ram_role`（无需管理密钥）
   - Kubernetes：`oidc_role_arn`（与 ServiceAccount 集成）
   - 函数计算：通过执行角色自动注入

2. **开发/测试环境**：
   - 配置文件：`~/.alibabacloud/credentials`
   - 环境变量：`ALIBABA_CLOUD_ACCESS_KEY_ID/SECRET`

3. **CI/CD 环境**：
   - OIDC（推荐）
   - 加密的环境变量 + 临时 STS Token

### 常见错误速查表

| 错误关键词 | 可能原因 | 快速解决 |
|-----------|---------|---------|
| `Unable to load credentials from any of the providers` | 所有凭证来源都失败 | 检查环境变量或配置文件是否设置 |
| `must not be null` | 必填字段缺失 | 补充对应字段（如 AccessKeyId、roleArn） |
| `The specified credentials file is empty` | 环境变量为空字符串 | `unset ALIBABA_CLOUD_CREDENTIALS_FILE` |
| `Client is not open` | 配置节未启用 | 设置 `enable = true` |
| `Failed to connect ECS Metadata Service` | 非 ECS 环境或网络不通 | 禁用 ECS 元数据或检查网络 |
| `The role name was not found` | 实例未绑定 RAM 角色 | 在 ECS 控制台绑定角色 |
| `not authorized to do this action` | 缺少 RAM 权限 | 授予对应 STS 操作权限 |
| `OIDCTokenFilePath is not exists` | Token 文件路径错误 | 检查文件路径和权限 |
| `Read timed out` | 网络超时 | 增加超时时间或检查网络 |
| `invalid type option` | 凭证类型拼写错误 | 使用正确的类型名（如 `access_key`） |

### 获取帮助

- **官方文档**：https://help.aliyun.com/document_detail/378659.html
- **GitHub Issues**：https://github.com/aliyun/credentials-java/issues
- **API 参考**：https://api.aliyun.com/
- **诊断平台**：https://api.aliyun.com/troubleshoot

---

**最后更新日期**：2025-12-07
