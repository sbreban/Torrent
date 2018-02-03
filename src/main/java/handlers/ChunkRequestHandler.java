package handlers;

import com.google.protobuf.ByteString;
import node.ChunkRequest;
import node.ChunkResponse;
import node.Message;
import node.Status;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChunkRequestHandler {

  private static final Logger logger = Logger.getLogger(ChunkRequestHandler.class.getName());

  public static Message handleChunkRequest(Message message, Map<ByteString, List<byte[]>> localFiles) {
    Message responseMessage = null;
    try {
      ChunkRequest chunkRequest = message.getChunkRequest();
      ByteString fileHash = chunkRequest.getFileHash();
      int chunkIndex = chunkRequest.getChunkIndex();
      List<byte[]> fileContent = localFiles.get(fileHash);
      ChunkResponse chunkResponse;
      if (fileContent != null) {
        byte[] chunk = fileContent.get(chunkIndex);
        chunkResponse = ChunkResponse.newBuilder().
            setStatus(Status.SUCCESS).
            setData(ByteString.copyFrom(chunk)).
            build();
      } else {
        chunkResponse = ChunkResponse.newBuilder().
            setStatus(Status.UNABLE_TO_COMPLETE).
            build();
      }
      responseMessage = Message.newBuilder().
          setType(Message.Type.CHUNK_RESPONSE).
          setChunkResponse(chunkResponse).
          build();
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return responseMessage;
  }

}
