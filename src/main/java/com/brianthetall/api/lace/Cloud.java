package com.brianthetall.api.lace;

public class Cloud {
    final String scheme;
    final String host;
    final int port;

    public static final Cloud cbtt = new Cloud("https","cloud.brianthetall.com",443);
    public static final Cloud btt = new Cloud("https","brianthetall.com",443);

    public Cloud(String scheme, String host, int port) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
    }

    public String getScheme() {
        return scheme;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
