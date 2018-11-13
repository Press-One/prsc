# prsc

PRESS.one DSL script interpreter

## Development Environment 

1. install Java 1.8+
2. install Clojure 1.9.0
3. install leiningen https://github.com/technomancy/leiningen

## Testing

lein test

## Usage

Start the service
  
    $ env apiroot=https://dev.press.one/api lein ring server

curl http://localhost:3001/parser?address=[:address]

curl http://localhost:3001/license/[:contractrid]?licensetype=Commercial


test contract rid : 

curl https://dev.press.one/api/blocks/txes?rIds=[:contractrid]


   { createdAt: '2018-09-06T07:09:35.000Z',
     address: 'ee6ddad145f681fd5bd19eca003c9d204d214471',
     rId:
      '2139270f89d70c8ed6b6e757bcc0bea1d0f65e6a494e91258af949c3d6708273',
     msghash:
      'e20ea9b1732d4717256efd51acee53b8667926ab3c56f3f544a29ab4768c055d',
     sig:
      '3423067b43d80a9cb7ffdfb810cd6ae9885e0f502211b9c2285a00d0711f3e547ee6588162867abf7d2b87a82f7cce540e4fa011a3705d4dc98e42bef28b54160',
     service: 'p1s',
     uuid:
      'e2/0e/e20ea9b1732d4717256efd51acee53b8667926ab3c56f3f544a29ab4768c055d.md',
     ver: 2 } }

## build and deploy

Run the service with prod env

```
lein with-profile prod ring server 
```

Build the service with prod env

```
lein with-profile prod ring uberjar 

java -jar target/prsc-0.1.0-SNAPSHOT-standalone.jar
```
