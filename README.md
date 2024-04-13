# 薜

![](https://img.shields.io/badge/%E5%AF%8C%E5%BC%BA-%E6%B0%91%E4%B8%BB-brightgreen)
![](https://img.shields.io/badge/%E6%96%87%E6%98%8E-%E5%92%8C%E8%B0%90-green)
![](https://img.shields.io/badge/%E8%87%AA%E7%94%B1-%E5%B9%B3%E7%AD%89-yellowgreen)
![](https://img.shields.io/badge/%E5%85%AC%E6%AD%A3-%E6%B3%95%E5%88%B6-yellow)
![](https://img.shields.io/badge/%E7%88%B1%E5%9B%BD-%E6%95%AC%E4%B8%9A-orange)
![](https://img.shields.io/badge/%E8%AF%9A%E4%BF%A1-%E5%8F%8B%E5%96%84-red)

Material Design 的哔哩哔哩非官方客户端

[![Android CI](https://github.com/storytellerF/bi/actions/workflows/android.yml/badge.svg)](https://github.com/storytellerF/bi/actions/workflows/android.yml)

>👆没有release，可以下载自动构建的apk

基于[bilimiao2](https://github.com/10miaomiao/bilimiao2) 制作，基于Jetpack Compose 构建（未来会迁移到KMM），以GPL 协议发布

## Build

克隆项目之后需要

```shell
# 确保你已经安装了git
git clone https://github.com/storytellerF/Bi.git
cd Bi
# 获取bilimiao2 的代码
git submodule update --init
```

需要安装jdk 17，Android Studio Preview Giraffe，Android SDK

1. 可以使用Android Studio 一键构建
2. 也可以使用命令行构建
    可以不安装Android Studio至少需要安装Android SDK

    ```shell
    sh gradlew build
    # 如需安装到手机
    sh gradlew installDebug
    ```