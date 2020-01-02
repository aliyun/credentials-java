English | [简体中文](./README-CN.md)

![](https://aliyunsdk-pages.alicdn.com/icons/AlibabaCloud.svg)



# Alibaba Cloud Credentials for Java
[![codecov](https://codecov.io/gh/aliyun/credentials-java/branch/master/graph/badge.svg)](https://codecov.io/gh/aliyun/credentials-java)
[![Travis Build Status](https://travis-ci.org/aliyun/credentials-java.svg?branch=master)](https://travis-ci.org/aliyun/credentials-java)
[![Appveyor Build Status](https://ci.appveyor.com/api/projects/status/6jxpwmhyfipagtge/branch/master?svg=true)](https://ci.appveyor.com/project/aliyun/credentials-java)

Alibaba Cloud Credentials for Java is a tool for Java developers to manage credentials.

This document introduces how to obtain and use Credentials for Java.

## Requirements

- The Alibaba Cloud Credentials for Java requires JDK 1.8 or later.


## Quick Examples

The following code example shows the three main steps to use Alibaba Cloud Credentials for Java :

1. Create and initialize a `Configuration` instance.

2. Create a `Credential`.


```java
package com.testprogram;
import com.aliyun.credentials.Credential;
import com.aliyun.credentials.Configuration;
import com.aliyun.credentials.utils.AuthConstant;

public class Main {
    public static void main(String[] args) {
        Configuration config = new Configuration();
        // use accessKeyId and accessKeySecret
        config.setType(AuthConstant.ACCESS_KEY);
        // set accessKeyId
        config.setAccessKeyId("your accessKeyId");
        // set accessKeySecret
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

## Issues
[Opening an Issue](https://github.com/aliyun/credentials-java/issues/new), Issues not conforming to the guidelines may be closed immediately.

## Changelog
Detailed changes for each release are documented in the [release notes](./ChangeLog.txt).

## Contribution
Please make sure to read the [Contributing Guide](./.github/PULL_REQUEST_TEMPLATE.md) before making a pull request.

## References
* [Alibaba Cloud Regions & Endpoints](https://developer.aliyun.com/endpoints)
* [OpenAPI Explorer](https://api.aliyun.com/)
* [Latest Release](https://github.com/aliyun/aliyun-openapi-java-sdk)

## License
[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Copyright 2009-present Alibaba Cloud All rights reserved.
