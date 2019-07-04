# prsc

PRESS.one DSL script interpreter

## Development Environment 

1. install JDK 11
2. install Clojure 1.9.0
3. install leiningen https://github.com/technomancy/leiningen

## Testing

lein test

## Usage

Generate a secp256k1 private key, replace the :privkey in the config.edn

Start the service
  
    $ env apiroot=https://press.one/api/v2 lein ring server

curl http://localhost:3001/parser?address=[:address]

curl http://localhost:3001/license/[:contractrid]?licensetype=Commercial


Example:

```
curl http://localhost:3001/v2/parser?address=6ed18d21690433f0410a20bdf5e829975e0da840b48c3cb8e962b17138f5288e
curl http://localhost:3001/v2/license/6ed18d21690433f0410a20bdf5e829975e0da840b48c3cb8e962b17138f5288e?licensetype=usage1
```
test contract rid : 
```
curl https://press.one/api/v2/blocks/6ed18d21690433f0410a20bdf5e829975e0da840b48c3cb8e962b17138f5288e
```

```
[
  {
    "id": "6ed18d21690433f0410a20bdf5e829975e0da840b48c3cb8e962b17138f5288e",
    "user_address": "becd34540fefeab83730ffb479e98ee12fa1337e",
    "type": "PUBLISH:1",
    "meta": "{\"mime\":\"text/prsc;charset=UTF-8\"}",
    "data": "{\"file_hash\":\"42cc05b72e9850151f5dd6cd86cdc0dd02b615272ce387cc6f92f41031e07a22\",\"code\":\"PRSC Ver 0.1\\nName 文章的授权购买\\nDesc 你购买的授权许可不具备排他性，不可再授权，不可转让。\\\\n不得直接转售、分销、共享、转让、出租这些数字内容，不得提供下载，不可嵌入服务器，不可包含在网页模板或设计模板中。\\\\n不得以明示或暗示的方式虚假声明你购买的该数字内容是由你或其它人所创建，或虚假声明你或其他人对你购买的该数字内容持有著作权。\\\\n不可将所购买的数字内容用于任何非法、淫秽、诽谤、虚假内容之中，或用于会被认为是非法、淫秽、诽谤、虚假内容的方式。\\nReceiver becd34540fefeab83730ffb479e98ee12fa1337e\\nLicense usage1 PRS:1 Terms: 个人授权许可，只能用于个人目的的转载、 引用、结集。\\\\n你可以全文或部分转载你购买的文本内容，或在不改变作者原意和立场的情况下编辑文本。任何情况下必须标明原文作者及出处。不标记原作者或出处通常会被视为剽窃行为。\\\\n不可用于商业用途（包括且不限于政府、 教育、 商业机构、非营利组织、社会团体、经营类网站及其它以营利为目的的组织机构或服务）。\\nLicense usage2 PRS:5 Terms: 商用授权许可，用于营利、 商业或经营目的转载、 引用、结集。如：\\\\n- 用于网站、软件、移动应用、社交媒体、邮件、电子书\\\\n- 用于印刷媒体如 杂志、 报纸、 书籍\\\\n- 用于广告、市场活动、宣传推广用途的内容\\\\n你可以全文或部分转载你购买的文本内容，或在不改变作者原意和立场的情况下编辑文本。任何情况下必须标明原文作者及出处。不标记原作者或出处通常会被视为剽窃行为。\"}",
    "hash": "038a2f1a0873b85caf08a028f06c57fde8072e0080dd4ae646ea4b721e6563d8",
    "signature": "69fa8109a42f23242865574d976c2778d69a021f164aa8841a433bc0837ed39f0ab9c209b25f6be065ac6de4c929bc1bacaba0f0877e5ac277b90f3c312c90e10",
    "blockNum": 4072600,
    "blockTransactionId": "d426558468e52126d1528ed2661a6d5411c550b19498417604180d32d9ba2ae6"
  }
]

```

```
curl https://press.one/api/v2/contracts/6ed18d21690433f0410a20bdf5e829975e0da840b48c3cb8e962b17138f5288e
```

```
{
  "file": {
    "id": 10945,
    "rId": "ebef207952349646817c237d240b57aab005e295190e2c6232b7816e294c5c08",
    "address": "becd34540fefeab83730ffb479e98ee12fa1337e",
    "msghash": "67d4a2c6b7810aa0b4399b38bc85dd5704b45640b2952fd03a8f65445a35e786",
    "title": "穿越寒冬的独行者",
    "description": "2018年有很多故事可以写，但是到最后一天的时候，让我选一件事来写，我想写的是duckduckgo这个搜索引擎。选择它是有原因的，这个搜索引擎创始于2008年，正好是第10个年头。 即使在今天，听说过这个搜索引擎的人也不多。上个月（2018.11），它的每日搜索量第一次超过了3000万次，很多科技媒体用非常小的版面报道过这件事。中文也有报道，基本上就是“一句话新闻”这样的待遇，没人多想什么。这不意外，每天3000万搜索听起来不小，但是放在整个搜索市场可以算的上微不足道。做为对比，Google早就不再...",
    "originUrl": "",
    "cacheUrl": "https://static.press.one/67/d4/67d4a2c6b7810aa0b4399b38bc85dd5704b45640b2952fd03a8f65445a35e786.md",
    "mimeType": "text/markdown",
    "createdAt": "2018-12-31T07:19:40.000Z",
    "source": "",
    "projectId": null,
    "rewardAmount": "184.529000000000000000000000000000",
    "rewardCount": 47
  },
  "contract": {
    "createdAt": "2018-12-31T07:20:04.000Z",
    "code": "PRSC Ver 0.1\nName 文章的授权购买\nDesc 你购买的授权许可不具备排他性，不可再授权，不可转让。\\n不得直接转售、分销、共享、转让、出租这些数字内容，不得提供下载，不可嵌入服务器，不可包含在网页模板或设计模板中。\\n不得以明示或暗示的方式虚假声明你购买的该数字内容是由你或其它人所创建，或虚假声明你或其他人对你购买的该数字内容持有著作权。\\n不可将所购买的数字内容用于任何非法、淫秽、诽谤、虚假内容之中，或用于会被认为是非法、淫秽、诽谤、虚假内容的方式。\nReceiver becd34540fefeab83730ffb479e98ee12fa1337e\nLicense usage1 PRS:1 Terms: 个人授权许可，只能用于个人目的的转载、 引用、结集。\\n你可以全文或部分转载你购买的文本内容，或在不改变作者原意和立场的情况下编辑文本。任何情况下必须标明原文作者及出处。不标记原作者或出处通常会被视为剽窃行为。\\n不可用于商业用途（包括且不限于政府、 教育、 商业机构、非营利组织、社会团体、经营类网站及其它以营利为目的的组织机构或服务）。\nLicense usage2 PRS:5 Terms: 商用授权许可，用于营利、 商业或经营目的转载、 引用、结集。如：\\n- 用于网站、软件、移动应用、社交媒体、邮件、电子书\\n- 用于印刷媒体如 杂志、 报纸、 书籍\\n- 用于广告、市场活动、宣传推广用途的内容\\n你可以全文或部分转载你购买的文本内容，或在不改变作者原意和立场的情况下编辑文本。任何情况下必须标明原文作者及出处。不标记原作者或出处通常会被视为剽窃行为。",
    "receiverName": "霍炬",
    "receiverAvatar": "https://static.press.one/ee/19/ee19a6825c9c90f6405840d40c63d900.png",
    "version": "0.1",
    "name": "文章的授权购买",
    "description": "你购买的授权许可不具备排他性，不可再授权，不可转让。\\n不得直接转售、分销、共享、转让、出租这些数字内容，不得提供下载，不可嵌入服务器，不可包含在网页模板或设计模板中。\\n不得以明示或暗示的方式虚假声明你购买的该数字内容是由你或其它人所创建，或虚假声明你或其他人对你购买的该数字内容持有著作权。\\n不可将所购买的数字内容用于任何非法、淫秽、诽谤、虚假内容之中，或用于会被认为是非法、淫秽、诽谤、虚假内容的方式。",
    "licenses": [
      {
        "type": "usage1",
        "currency": "PRS",
        "price": "1",
        "termtext": "个人授权许可，只能用于个人目的的转载、 引用、结集。\\n你可以全文或部分转载你购买的文本内容，或在不改变作者原意和立场的情况下编辑文本。任何情况下必须标明原文作者及出处。不标记原作者或出处通常会被视为剽窃行为。\\n不可用于商业用途（包括且不限于政府、 教育、 商业机构、非营利组织、社会团体、经营类网站及其它以营利为目的的组织机构或服务）。"
      },
      {
        "type": "usage2",
        "currency": "PRS",
        "price": "5",
        "termtext": "商用授权许可，用于营利、 商业或经营目的转载、 引用、结集。如：\\n- 用于网站、软件、移动应用、社交媒体、邮件、电子书\\n- 用于印刷媒体如 杂志、 报纸、 书籍\\n- 用于广告、市场活动、宣传推广用途的内容\\n你可以全文或部分转载你购买的文本内容，或在不改变作者原意和立场的情况下编辑文本。任何情况下必须标明原文作者及出处。不标记原作者或出处通常会被视为剽窃行为。"
      }
    ],
    "receiver": "becd34540fefeab83730ffb479e98ee12fa1337e",
    "rId": "6ed18d21690433f0410a20bdf5e829975e0da840b48c3cb8e962b17138f5288e",
    "total": 5
  }
}
```


## build and deploy

Run the service with prod env

```
lein with-profile prod ring server 
```

Build the service with prod env

```
lein with-profile prod ring uberjar 

java -jar target/prsc-0.1.1-SNAPSHOT-standalone.jar
```

## for docker testing

docker build -t dh.press.one/pressone/prsc .
docker run --rm -e apiroot=https://dev.press.one/api/v2 --name prsc -p 3001:3001 dh.press.one/pressone/prsc
