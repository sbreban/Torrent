package util;

import node.Message;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

public class MessageUtil {

  private static final Logger logger = Logger.getLogger(MessageUtil.class.getName());

  public static byte[] getMessageBytes(Socket socket) throws IOException {
    byte[] size = new byte[4];
    InputStream clientInputStream = new BufferedInputStream(socket.getInputStream());
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

  public static void sendMessage(Socket socket, Message message) throws IOException {
    OutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
    byte[] responseMessageSize = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(message.toByteArray().length).array();
    outputStream.write(responseMessageSize);
    outputStream.write(message.toByteArray());
    outputStream.flush();
  }

}
