人間にやさしいスクリプトをアセンブリ言語に変換するやつ(仮)
----------------------------------------------------------

これは何?
=========

人間にやさしいスクリプトをアセンブリ言語に変換するやつである。

使い方
======

### コマンド

```
java -jar ScriptToAssembly.jar [オプション]
```

### オプション

* `--input ファイル名` または `-i ファイル名`
  * 入力(スクリプト)のファイル名を指定する。複数指定可能である。指定しない場合は標準入力から読み込む。
* `--output ファイル名` または `-o ファイル名`
  * 出力(アセンブリ言語)のファイル名を指定する。指定しない場合は標準出力に出力する。
* `--libdir ディレクトリ名` または `-l ディレクトリ名`
  * ライブラリを検索するディレクトリ名を指定する。複数指定可能である。
* `--target ターゲット名` または `-t ターゲット名`
  * ターゲットを指定する。`IA32`(デフォルト)と`LPC1114FN28`が有効である。
* `--ttl TTL値`
  * `include`で許される最大の深さを指定する。デフォルトは10。
* `--debug`
  * 例外発生時にスタックトレースを出力する。
* `--help` または `-h`
  * ヘルプを出力する。
* `--version` または `-v`
  * バージョン情報を出力する。

スクリプトの文法
================

### ファイルの取り込み

C言語でいうところの`#include`的なやつ。

#### 現在のファイルを基準にしたパスのファイルを取り込む

```
include パス
```

#### ライブラリ用ディレクトリとして指定した場所を基準にしたパスのファイルを取り込む

```
uselib パス
```

### 型

#### 基本型

* 1バイト整数
  * `int8` : 1バイト符号つき整数
  * `uint8` : 1バイト符号なし整数
  * `byte` : 1バイト符号なし整数 (`uint8`と同じ)
  * `char` : 1バイト符号なし整数 (`uint8`と同じ)
* 2バイト整数
  * `int16` : 2バイト符号つき整数
  * `uint16` : 2バイト符号なし整数
* 4バイト整数
  * `int32` : 4バイト符号付き整数
  * `uint32` : 4バイト符号なし整数
  * `int` : 4バイト符号付き整数 (`int32`と同じ)
  * `uint` : 4バイト符号なし整数 (`uint32`と同じ)
* システム整数
  * `sysint` : ターゲットで都合の良いサイズの符号付き整数
  * `sysuint` : ターゲットで都合の良いサイズの符号なし整数
  * `ptrint` : ポインタ(アドレス)と同じサイズの符号付き整数
  * `ptruint` : ポインタ(アドレス)と同じサイズの符号なし整数
  * `funcint` : 関数と同じサイズの符号付き整数
  * `funcuint` : 関数と同じサイズの符号なし整数
* その他
  * `none` : データなし (C言語の`void`的なやつ)
  * `func(戻り値の型)` : 関数

「関数」は関数ポインタのようなものである。
一般のポインタと違って、演算(加減算、大小比較)は許可されない。

#### 派生型

* `*型` : `型`を指すポインタ
* `[要素数を表す定数式]型` : `型`を要素とする配列

基本型を取り出すためにする操作を左から順番に書く、というイメージ。

##### 例

* `int` : 4バイト符号付き整数(以下、整数)
* `*int` : 整数を指すポインタ
* `[5]int` : 整数の配列
* `[5]*int` : 整数を指すポインタの配列
* `*[5]int` : 整数の配列を指すポインタ
* `[10][5]int` : 整数の配列の配列 (要素にアクセスするときは、`hoge@(0～9)@(0～4)`のようになる)
* `func(int)` : 整数を返す関数
* `*func(int)` : 整数を返す関数を指すポインタ
* `[5]func(int)` : 整数を返す関数の配列

### 宣言・定義

宣言は、変数や関数の存在のみを示すもので、実体の作成を指示しない。
定義は、変数や関数の実体の作成を指示する。

関数内での宣言や定義はローカル(その関数内で有効)、
関数外での宣言や定義はグローバル(プログラム全体で有効)である。
ただし、「プログラム全体」といっても宣言や定義の前では使用できない。

型が同じ宣言は、同じ名前で何回でもしていい。
定義の後でも同じ型なら同じ名前の宣言をしていい。
同じ名前で型が違う宣言はエラーである。

ローカル変数(引数を含む)の定義はできるが、宣言はできない。
ローカルでの宣言がローカル変数と同じ名前の場合、エラーである。
ローカル変数の定義がグローバルの宣言や定義と同じ名前の場合、
ローカル変数が優先される。

#### 定数の定義

##### 定数を定義する

```
define 識別子 型 : 定数式
```

定数式とは、変数や関数の呼び出しを含まない式である。
型は整数型のみとする。(`none`、ポインタ、関数などはダメ)

#### 変数の定義

##### グローバル変数を定義する

(関数の定義中以外で)

```
var 識別子 型
```

##### ローカル変数を定義する

(関数の定義中で)

```
var 識別子 型
```

##### アドレスを指定した(グローバル)変数を定義する

```
address 識別子 型 : アドレスを表す定数式
```

#### 変数の宣言

##### グローバル変数を宣言だけして定義しない

```
vardeclare 識別子 型
```

#### 関数の定義

このスクリプトでの「関数」は、
C言語的な意味での関数、すなわちサブルーチンのようなものである。

##### 関数の定義

```
function 識別子 戻り値の型
  引数の宣言(0個以上)
  関数の中身
endfunction
```

引数は、関数の中で最初に書かれたものほど前の引数となる。

##### 引数の宣言

```
param 識別子 型
```

##### 引数の宣言(シンタックスシュガー)

```
argument 識別子 型
```

#### 関数の宣言

循環呼び出しがある場合などに用いる。

##### 関数の宣言

```
funcdeclare 識別子 型
```

### 条件分岐・ループ

#### 条件分岐

(関数宣言の中で)

```
if 式1
  式1が0以外だったときにする処理
elseif 式2
  式1が0で、式2が0以外だったときにする処理
else
  式1も式2も0だったときにする処理
endif
```

`elseif`は0個以上何個でも使えることができる。
`else`は省略可能で、使うなら必ず最後(`elseif`の後)である。

#### whileループ

(関数宣言の中で)

```
while 式
  式が0以外だったときにする処理
endwhile
```

#### 無限ループ

(関数宣言の中で)

```
loop
  繰り返す処理
endloop
```

#### ループ脱出

nは定数式でないといけない。

##### ループを抜け、次の処理に行く

```
break
```

##### 内側からn番目(1-origin)のループを抜け、次の処理に行く

```
break n
```

##### ループの最初の処理に戻る

```
continue
```

##### 内側からn番目(1-origin)のループの最初の処理に戻る

```
continue n
```

### 関数から戻る

#### 関数から戻る

```
return
```

#### 戻り値を指定して関数から戻る

```
return 戻り値を表す式
```

### 式

#### リテラル

##### 数値リテラル

数値リテラルは、数値である。
「文字列の最初のバイト」は`char`型、それ以外は`sysint`型である。

* 10進: `123`
*  8進: `0123`
*  2進: `0b1101`
* 16進: `0x123`
* 文字列の最初のバイト : `'文字列'` (文字列が0文字の場合文法エラー)

##### 文字列リテラル

文字列リテラルは、1バイトの符号なし整数(`char`型)の配列とみなせる。
`""`で囲まれる。

#### 識別子

識別子を用いて、変数や関数にアクセスできる。

##### 識別子に利用可能な文字

* 半角英アルファベット大文字、小文字
* 数字 (識別子の1文字目には不可)
* アンダーバー

#### 演算子

##### 単項演算子

* `-x` : xに(-1)を掛ける
* `+x` : x
* `!x` : xが0以外なら0、xが0なら1
* `~x` : xのビット反転
* `*x` : ポインタxが指すデータ
* `&x` : xのアドレス
* `#x` : xのバイト数(C言語の`sizeof`演算子)
* `{型}x` : xを型とみなしたデータ(キャスト)

##### 二項演算子

###### 計算

シフトやローテートを除く整数同士の計算結果の型は、サイズが大きい方の型に合わせる。
サイズが同じ場合は、少なくとも1方が符号なしなら符号なし、両方符号付きなら符号付きにする。

シフトやローテートの計算結果の型は、シフトやローテートをされる数、すなわち左辺と同じである。

ポインタと整数の足し算の場合は、ポインタの型にする。
整数はポインタが指す型のサイズを掛けてからポインタに足す。

ポインタ同士の引き算の場合は、ポインタと同じサイズの符号付き整数型(`ptrint`型)にする。

* `x+y` : 足し算
* `x-y` : 引き算
* `x*y` : 掛け算
* `x/y` : 割り算の商
* `x%y` : 割り算の余り
* `x@y` : 配列xのy番目の要素
* `x&y` : ビット論理積
* `x|y` : ビット論理和
* `x^y` : ビット排他的論理和
* `x<<y` : ビット左シフト
* `x>>y` : ビット算術右シフト
* `x>>>y` : ビット論理右シフト
* `x^<y` : ビット左ローテート
* `x>^y` : ビット右ローテート

###### 比較

条件を満たせば1、満たさなければ0になる。`sysint`型である。

* `x>y` : xがyより大きい
* `x>=y` : xがy以上
* `x<y` : xがyより小さい
* `x<=y` : xがy以下
* `x==y` : xがyと等しい
* `x!=y` : xがyと等しくない

###### 代入

代入式の型は左辺(代入されるやつ)の型になる。

* `x=y` : xにyを代入

###### 論理(短絡評価あり)

結果は`sysint`型である。

* `x&&y` : 「xが0でなく、かつyが0でない」なら1、そうでないなら0
* `x||y` : 「xが0でないか、yが0でない」なら1、そうでないなら0

###### 関数呼び出し

* `x()` : 関数xを引数なしで呼び出す
* `x(y)` : 関数xを1個の引数yで呼び出す
* `x(y,z)` : 関数xを2個の引数y,zで呼び出す
* 以下同様

##### 演算子の優先順位と結合

```
↑高い
左結合: ()
右結合: 単項- 単項+ ! ~ 単項* 単項& # {型}
左結合: @
左結合: 二項* / %
左結合: 二項+ 二項-
左結合: << >> >>> ^< >^
左結合: 二項&
左結合: ^
左結合: |
左結合: > >= < <=
左結合: == !=
左結合: &&
左結合: ||
右結合: =
↓低い
```

ほぼC言語と同じですが
C言語とは違い、ビット演算子(&, |, ^)の優先順位が比較演算子より高いです。
`a & 3 == 3`と書いた時、`a`と`3 == 3`、すなわち1のANDではなく`a & 3`と`3`の等価判定ができます。

### コメント

1行コメントはコマンド扱いなので、行の途中からコメントにすることはできない。

#### 1行コメント

```
comment コメントの内容
```

#### 1行コメント (シンタックスシュガー)

```
# コメントの内容
```
