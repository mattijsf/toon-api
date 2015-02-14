[![Build Status](https://travis-ci.org/travis-repos/chirp-org-production.svg?branch=master)](https://travis-ci.org/travis-repos/chirp-org-production)

# Toon API
A reversed engineered java API for the Eneco Toon thermostat.

## Disclaimer
Use at your own risk. 

## Features
Implemented so far:

* Set program: [Home, Away, Sleep, Comfort]
* Set temperature: e.g: '19.5'
* more to come


The java API itself is based on [Apache HttpClient 4.3.x](http://hc.apache.org/httpcomponents-client-4.3.x/index.html):
```java
new ToonClient(HttpClients.createDefault());
```

Or, if you want to use [OkHttp](https://github.com/square/okhttp/tree/master/okhttp-apache)
```java
new ToonClient(new OkApacheClient());
```

On Android:
```java
new ToonClient(new DefaultHttpClient())
```



## Usage

### ToonClient

Example:
```java
// Create a toon client instance using the default ToonMemoryPersistenceHandler.
// Remember that when you use the default persistence handler you have to authenticate
// with a plain-text username/password every time you create an instance of ToonClient.
ToonClient toon = new ToonClient(HttpClients.createDefault());

// Alternatively (better), create toon client instance using an actual persistence storage like
// the built-in ToonFilePersistenceHandler. This will store a hashed password once you performed
// the initial authentication. Once authenticated you won't have to authenticate again if you
// use the same ToonFilePersistenceHandler when you create a new ToonClient instance.
ToonClient toon = new ToonClient(HttpClients.createDefault(), new ToonFilePersistenceHandler(file));
```

By default the `ToonClient` will use a built-in `ToonMemoryPersistenceHandler` but it is not recommended when using the API in real life. Basically the handler stores the username / password-hash returned by the external Toon API upon the initial authentication. When the authentication tokens (that are used to communicate with the external Toon API) expire or become invalid the ToonFilePersistenceHandler is consulted for credentials in order to renew the authentication tokens automatically. There is a `ToonFilePersistenceHandler` (see above exmaple) that you could use to get started, but you can create your own by implementing the `ToonPersistenceHandler` interface.

### Authenticate
Authentication should be performed using the same account you use for the actual Toon device.

```java
toon.authenticate("email@email.com", "some_password");
```
You can check the result boolean if the authentication succeeded but when the authentication has failed for whatever reason a ToonLoginFailedException will be thrown.

You can check wether or not authentication is required:
```java
if(!toon.hasCredentials()){
  ...
}
```
Note that `hasCredentials` will always return true when you used a persistence storage for your `ToonClient` instance and you already authenticated successfully in a previous session.

### Change the temperature
```java
boolean success = toon.setTemperature(20.5f);
```

### Change the program
```java
boolean success = toon.setSchemeState(ToonSchemeState.HOME);
boolean success = toon.setSchemeState(ToonSchemeState.AWAY);
boolean success = toon.setSchemeState(ToonSchemeState.COMFORT);
boolean success = toon.setSchemeState(ToonSchemeState.SLEEP);
```
## Known issues

You can expect SSL issues using java 6 on a Mac / (some) Linux distributions. I got messages like:

```java
javax.net.ssl.SSLException: java.lang.RuntimeException: Could not generate DH keypair
...
java.security.InvalidAlgorithmParameterException: Prime size must be multiple of 64, and can only range from 512 to 1024 (inclusive)
```

So far I've only been able to work around it by using Java 7. More on the subject here: http://stackoverflow.com/questions/6851461/java-why-does-ssl-handshake-give-could-not-generate-dh-keypair-exception
