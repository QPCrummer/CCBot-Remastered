package net.ddns.crummercraft.servers;

public class ServerJson {
    private String name;
    private String version;
    private String path;
    private int port;
    private String ip;
    private boolean isMainServer;
    private boolean isProxy;

    public ServerJson(String name, String version, String path, int port, String ip, boolean isMainServer, boolean isProxy) {
        this.name = name;
        this.version = version;
        this.path = path;
        this.port = port;
        this.ip = ip;
        this.isMainServer = isMainServer;
        this.isProxy = isProxy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean isMainServer() {
        return isMainServer;
    }

    public void setMainServer(boolean mainServer) {
        this.isMainServer = mainServer;
    }

    public boolean isProxy() {
        return isProxy;
    }

    public void setProxy(boolean proxy) {
        this.isProxy = proxy;
    }

    public String toString() {
        return "Server [ name: " + name + ", version: " + version + ", path: " + path + ", ip: " + ip +":"+port + " ]";
    }

    public String toStringWithoutPath() {
        return name + ": version: " + version + ", ip: " + ip +":"+port;
    }
}