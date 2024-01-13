# 微信抢红包

## change history
- 2024-01-13 
  - 测试机型小米10pro，测试微信版本 Google play 最新版。

## 代码说明
遇到微信适配问题，请自行查找控件id。
```shell
adb shell uiautomator dump
```
会返回一个页面的所有控件信息

```shell
adb pull /sdcard/window_dump.xml
```
将上一步生成的文件 pull 出来。