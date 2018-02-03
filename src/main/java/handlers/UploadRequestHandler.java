package handlers;

import com.google.protobuf.ByteString;
import node.*;
import util.ChunkInfoUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UploadRequestHandler {

  private static final Logger logger = Logger.getLogger(UploadRequestHandler.class.getName());

  public static Message handleUploadRequest(Message message) {
    Message responseMessage = null;
    try {
      UploadRequest uploadRequest = message.getUploadRequest();
      String fileName = uploadRequest.getFilename();
      ByteString data = uploadRequest.getData();
      byte[] bytes = data.toByteArray();
      try (FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
        fileOutputStream.write(bytes);
      } catch (IOException e) {
        logger.log(Level.SEVERE, e.getMessage());
      }

      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest;

      List<ChunkInfo> chunkInfos = ChunkInfoUtil.getChunkInfos(bytes, md);
      Status status = Status.SUCCESS;

      md.update(bytes);
      digest = md.digest();

      FileInfo fileInfo = FileInfo.newBuilder().
          setHash(ByteString.copyFrom(digest)).
          setSize(data.size()).
          setFilename(fileName).
          addAllChunks(chunkInfos).
          build();
      UploadResponse uploadResponse = UploadResponse.newBuilder().
          setStatus(status).
          setFileInfo(fileInfo).
          build();
      responseMessage = Message.newBuilder().
          setType(Message.Type.UPLOAD_RESPONSE).
          setUploadResponse(uploadResponse).
          build();
    } catch (NoSuchAlgorithmException e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return responseMessage;
  }

}
