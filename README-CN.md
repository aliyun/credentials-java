[English](./README.md) | 简体中文

![](https://aliyunsdk-pages.alicdn.com/icons/AlibabaCloud.svg)



# Alibaba Cloud Credentials for Java
[![Travis Build Status](https://travis-ci.org/aliyun/credentials-java.svg?branch=master)](https://travis-ci.org/aliyun/credentials-php)
[![Appveyor Build Status](https://ci.appveyor.com/api/projects/status/6jxpwmhyfipagtge/branch/master?svg=true)](https://ci.appveyor.com/project/aliyun/credentials-java)
[![codecov](https://codecov.io/gh/aliyun/credentials-java/branch/master/graph/badge.svg)](https://codecov.io/gh/aliyun/credentials-java)

Alibaba Cloud Credentials for Java 是帮助 Java 开发者管理凭据的工具。

本文将介绍如何获取和使用 Credentials for Java。

## 环境要求
1.  Alibaba Cloud Credentials for Java 需要1.8以上的JDK。

## 快速使用

以下这个代码示例向您展示了调用 Alibaba Cloud Credentials for Java 的2个主要步骤：
1. 创建 `Configuration`实例并初始化。
2. 创建 `Credential`。


```java
package com.testprogram;
import com.aliyun.credentials.Credential;
import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.utils.AuthConstant;

public class Main {
    public static void main(String[] args) {
        Configuration config = new Configuration();
        // 使用 accessKeyId 和 accessKeySecret 方式
        config.setType(AuthConstant.ACCESS_KEY);
        // 设置 accessKeyId
        config.setAccessKeyId("your accessKeyId");
        // 设置 accessKeySecret
        config.setAccessKeySecret("your accessKeySecret");
        Credential credential;
        try {
            credential = new Credential(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 问题
[提交 Issue](https://github.com/aliyun/credentials-java/issues/new)，不符合指南的问题可能会立即关闭。

## 发行说明
每个版本的详细更改记录在[发行说明](./ChangeLog.txt)中。

## 贡献
提交 Pull Request 之前请阅读[贡献指南](./.github/PULL_REQUEST_TEMPLATE.md)。

## 相关
* [阿里云服务 Regions & Endpoints](https://developer.aliyun.com/endpoints)
* [OpenAPI Explorer](https://api.aliyun.com/)
* [最新源码](https://github.com/aliyun/aliyun-openapi-java-sdk)

## 许可证
[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Copyright 2009-present Alibaba Cloud All rights reserved.
