package node;

import java.util.Objects;

public class NodeConfiguration {
  private String addr;
  private int port;

  public NodeConfiguration(String addr, int port) {
    this.addr = addr;
    this.port = port;
  }

  public String getAddr() {
    return addr;
  }

  public int getPort() {
    return port;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NodeConfiguration that = (NodeConfiguration) o;
    return port == that.port &&
        Objects.equals(addr, that.addr);
  }

  @Override
  public int hashCode() {

    return Objects.hash(addr, port);
  }
}
