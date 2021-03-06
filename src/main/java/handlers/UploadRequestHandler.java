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
    UploadResponse.Builder builder = UploadResponse.newBuilder();

    UploadRequest uploadRequest = message.getUploadRequest();
    String fileName = uploadRequest.getFilename();

    if (fileName == null || fileName.isEmpty()) {
      logger.severe("Filename is empty");
      builder.setStatus(Status.MESSAGE_ERROR);
    } else if (fileNameToHash.containsKey(fileName)) {
      logger.fine("Already have the file");
      builder.setStatus(Status.SUCCESS);
    } else {
      try {

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

        FileInfo fileInfo = FileInfo.newBuilder().
            setHash(ByteString.copyFrom(digest)).
            setSize(data.size()).
            setFilename(fileName).
            addAllChunks(chunkInfos).
            build();

        builder.setStatus(Status.SUCCESS);
        builder.setFileInfo(fileInfo);
      } catch (NoSuchAlgorithmException e) {
        logger.log(Level.SEVERE, e.getMessage());
      }
    }

    return Message.newBuilder().
        setType(Message.Type.UPLOAD_RESPONSE).
        setUploadResponse(builder.build()).
        build();
  }

}
