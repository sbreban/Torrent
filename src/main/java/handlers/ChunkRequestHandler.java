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
    ChunkResponse.Builder builder = ChunkResponse.newBuilder();
    try {
      ChunkRequest chunkRequest = message.getChunkRequest();
      ByteString fileHash = chunkRequest.getFileHash();
      int chunkIndex = chunkRequest.getChunkIndex();

      if (fileHash.toByteArray().length != 16 || chunkIndex < 0) {
        logger.severe("Invalid file hash or chunk index");
        builder.setStatus(Status.MESSAGE_ERROR);
      } else {
        List<byte[]> fileContent = localFiles.get(fileHash);
        if (fileContent != null && fileContent.size() > 0) {
          byte[] chunk = fileContent.get(chunkIndex);
          builder.setStatus(Status.SUCCESS).
              setData(ByteString.copyFrom(chunk));
        } else {
          builder.setStatus(Status.UNABLE_TO_COMPLETE);
        }
      }

    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
      builder.setStatus(Status.PROCESSING_ERROR);
    }

    return Message.newBuilder().
        setType(Message.Type.CHUNK_RESPONSE).
        setChunkResponse(builder.build()).
        build();
  }

}
