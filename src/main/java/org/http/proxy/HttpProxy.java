package org.http.proxy;

import org.apache.log4j.Logger;
import org.http.proxy.aspects.IProxyPostprocessor;
import org.http.proxy.aspects.IProxyInterceptor;
import org.http.proxy.aspects.IProxyPreprocessor;
import org.http.proxy.aspects.impl.PacketSnap;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class HttpProxy {
    private static final Logger logger = Logger.getLogger(HttpProxy.class);

    private ServerSocket serverSocket;
    private final List<Socket> clientSocketList = Collections.synchronizedList(new LinkedList<Socket>());
    private IProxyPreprocessor preprocessor;
    private IProxyInterceptor interceptor;
    private IProxyPostprocessor postprocessor;

    public HttpProxy(String host, int port, IProxyPreprocessor preprocessor, IProxyInterceptor interceptor, IProxyPostprocessor postprocessor)
            throws IOException {
        this.preprocessor = preprocessor;
        this.interceptor = interceptor;
        this.postprocessor = postprocessor;
        if (host == null || host.length() < 1 || "null".equalsIgnoreCase(host)) {
            serverSocket = new ServerSocket(port);
        } else {
            serverSocket = new ServerSocket();
            SocketAddress address = new InetSocketAddress(host, port);
            serverSocket.bind(address);
        }
    }

    public HttpProxy(String host, int port) throws IOException {
        this(host, port, null, null, null);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: HttpProxy host port");
            System.out.println("host may be null,but must explicitly specified as 'null'");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        HttpProxy proxy = new HttpProxy(host, port, null, null, new PacketSnap());
        System.out.println("Proxy started on " + host + ":" + port);
        proxy.start();
        proxy.stop();
    }

    public void start() {
        while (serverSocket.isBound()) {
            synchronized (clientSocketList) {
                Socket clientSocket;
                try {
                    clientSocket = serverSocket.accept();
                    clientSocketList.add(clientSocket);
                    logger.info(String.format("connection from %s", clientSocket.getRemoteSocketAddress()));
                    TaskExecutor.submitTask(new ProxyThread(this, clientSocket));
                } catch (IOException e) {
                    logger.error("error occurs initiating a new connection");
                }
            }
        }
    }

    // stop proxy mode
    public void stop() throws IOException {
        TaskExecutor.shutdown();
        closeAllClientSocket();
        serverSocket.close();
    }

    private void closeAllClientSocket() throws IOException {
        synchronized (clientSocketList) {
            for (Socket socket : clientSocketList) {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
            clientSocketList.clear();
        }
    }

    public IProxyPreprocessor getPreprocessor() {
        return preprocessor;
    }

    public IProxyInterceptor getInterceptor() {
        return interceptor;
    }

    public IProxyPostprocessor getPostprocessor() {
        return postprocessor;
    }
}
