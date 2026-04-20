# 【非公式】RosettaEnchantTable(NeoForge版)
https://www.curseforge.com/minecraft/mc-mods/rosettaenchanttable にNeoForge 1.21.1 用がなかったので作成。

# ビルドメモ

ソース一式を clone する。  
ソースのあるディレクトリに移動する。

Java 21 が必要なのでインストールする。  
mise を使用しているなら `$ mise install` のみで OK.  

OpenJDK 21 にパスが通っているか確認する。

```sh
$ java -version
openjdk version "21.0.2" 2024-01-16
OpenJDK Runtime Environment (build 21.0.2+13-58)
OpenJDK 64-Bit Server VM (build 21.0.2+13-58, mixed mode, sharing)
```

`gradlew` のあるディレクトリに移動して `$ ./gradlew build` でビルドする。  

ビルドが終了したら `$SRC_ROOT/build/libs/` を見ると `rosettaenchanttable-1.0.0.jar` が生成されているので確認する。

