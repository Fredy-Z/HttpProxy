## HTTP Proxy

A http proxy written with socket.

#### USAGE:
```
HttpProxy proxy = new HttpProxy(host, port, null, null, null);
proxy.start();
```

We can add preprocessor, interceptor and postprocessor to change or monitor the traffic in different stages. 
Some default implementations are provided, you can also write your own.
```
RemoteMapPreprocessor remoteMapPreprocessor = new RemoteMapPreprocessor(new HashMap<>());
LocalMapInterceptor localMapInterceptor = new LocalMapInterceptor(new HashMap<>());
PacketSnap packetSnap = new PacketSnap();
HttpProxy proxy = new HttpProxy(host, port, remoteMapPreprocessor, localMapInterceptor, packetSnap);
proxy.start();
```
