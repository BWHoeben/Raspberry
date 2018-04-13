package UDP;

import java.net.InetAddress;

public class Destination {

    private int port;
    private InetAddress address;

    public Destination(int port, InetAddress address) {
        this.port = port;
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public InetAddress getAddress() {
        return address;
    }

}
