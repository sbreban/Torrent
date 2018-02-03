package util;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class MessageUtil {

  private static final Logger logger = Logger.getLogger(MessageUtil.class.getName());

  public static byte[] getMessageBytes(Socket socket) throws IOException {
    byte[] size = new byte[4];
    InputStream clientInputStream = socket.getInputStream();
    int read = clientInputStream.read(size, 0, 4);
    ByteBuffer wrapped = ByteBuffer.wrap(size); // big-endian by default
    int messageSize = wrapped.getInt();
    byte[] buffer = new byte[messageSize];
    logger.fine(socket + " " + messageSize + " " + read);
    read = clientInputStream.read(buffer, 0, messageSize);
    if (read == messageSize) {
      return buffer;
    }
    return null;
  }

}
