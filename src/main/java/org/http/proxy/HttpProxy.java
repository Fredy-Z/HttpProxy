package org.http.proxy;

import org.apache.log4j.Logger;
import org.http.proxy.callbacks.IProxyCallback;
import org.http.proxy.interceptors.IProxyInterceptor;
import org.http.proxy.preprocessors.IProxyPreprocessor;
import org.http.proxy.tasks.TaskExecutor;

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
    private List<Socket> clientSocketList = Collections.synchronizedList(new LinkedList<Socket>());
    private IProxyPreprocessor preprocessor;
    private IProxyInterceptor interceptor;
    private IProxyCallback callbacker;

    public HttpProxy(String host, int port, IProxyPreprocessor preprocessor, IProxyInterceptor interceptor, IProxyCallback callbacker)
            throws IOException {
        this.preprocessor = preprocessor;
        this.interceptor = interceptor;
        this.callbacker = callbacker;
        if (host == null || host.length() < 1 || "null".equalsIgnoreCase(host)) {
            serverSocket = new ServerSocket(port);
        } else {
            serverSocket = new ServerSocket();
            SocketAddress address = new InetSocketAddress(host, port);
            serverSocket.bind(address);
        }
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
            if (clientSocketList != null && clientSocketList.size() > 0) {
                for (Socket socket : clientSocketList) {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                }
            }
        }
    }

    public void preprocessOnProxyThread(ProxyThread thread) {
        if (preprocessor != null) {
            try {
                preprocessor.on(thread);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public void interceptOnProxyThread(ProxyThread thread) {
        if (interceptor != null) {
            try {
                interceptor.on(thread);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public void callbackOnProxyThread(ProxyThread thread) {
        if (callbacker != null) {
            try {
                callbacker.on(thread);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: HttpProxy host port");
            System.out.println("host may be null,but must explicitly specified as 'null'");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        HttpProxy proxy = new HttpProxy(host, port, null, null, null);
        System.out.println("Proxy started on " + host + ":" + port);
        proxy.start();
        proxy.stop();
    }
}