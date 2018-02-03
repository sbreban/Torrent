package handlers;

import com.google.protobuf.ByteString;
import node.ChunkRequest;
import node.Message;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ChunkRequestHandler {

  private static final Logger logger = Logger.getLogger(ChunkRequestHandler.class.getName());

  public static Message handleChunkRequest(Message message) {
    Message responseMessage = null;
    try {
      ChunkRequest chunkRequest = message.getChunkRequest();
      ByteString fileHash = chunkRequest.getFileHash();
      int chunkIndex = chunkRequest.getChunkIndex();
      byte[] bytes = fileHash.toByteArray();
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return responseMessage;
  }

}
