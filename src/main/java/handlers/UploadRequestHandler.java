package handlers;

import com.google.protobuf.ByteString;
import node.*;
import util.ChunkInfoUtil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UploadRequestHandler {

  private static final Logger logger = Logger.getLogger(UploadRequestHandler.class.getName());

  public static Message handleUploadRequest(Message message, Map<ByteString, List<byte[]>> localFiles, Map<String, ByteString> fileNameToHash) {
    Message responseMessage = null;
    try {
      UploadRequest uploadRequest = message.getUploadRequest();
      String fileName = uploadRequest.getFilename();
      ByteString data = uploadRequest.getData();
      byte[] bytes = data.toByteArray();

      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] digest;
      md.update(bytes);
      digest = md.digest();
      List<byte[]> fileContent = new LinkedList<>();
      localFiles.put(ByteString.copyFrom(digest), fileContent);
      fileNameToHash.put(fileName, ByteString.copyFrom(digest));

      List<ChunkInfo> chunkInfos = ChunkInfoUtil.getChunkInfos(bytes, fileContent);
      Status status = Status.SUCCESS;

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
