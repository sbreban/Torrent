package handlers;

import com.google.protobuf.ByteString;
import node.*;
import util.ChunkInfoUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocalSearchRequestHandler {

  private static final Logger logger = Logger.getLogger(LocalSearchRequestHandler.class.getName());

  public static Message handleLocalSearchRequest(Message message, Map<ByteString, List<byte[]>> localFiles, Map<String, ByteString> fileNameToHash) {
    Message responseMessage = null;
    try {
      LocalSearchRequest localSearchRequest = message.getLocalSearchRequest();
      String regex = localSearchRequest.getRegex();
      List<String> foundFileNames = new ArrayList<>();
      for (String fileName : fileNameToHash.keySet()) {
        if (fileName.matches(regex)) {
          foundFileNames.add(fileName);
        }
      }
      List<FileInfo> fileInfos = new ArrayList<>();
      if (foundFileNames.size() > 0) {
        for (String foundFileName : foundFileNames) {
          ByteString fileHash = fileNameToHash.get(foundFileName);
          List<byte[]> fileContent = localFiles.get(fileHash);
          List<ChunkInfo> chunkInfos = ChunkInfoUtil.getChunkInfos(fileContent);
          logger.fine(chunkInfos.toString());

          FileInfo fileInfo = FileInfo.newBuilder().
              setHash(fileHash).
              setSize(chunkInfos.size()).
              setFilename(foundFileName).
              addAllChunks(chunkInfos).
              build();
          fileInfos.add(fileInfo);
        }
      }

      LocalSearchResponse localSearchResponse = LocalSearchResponse.newBuilder().
          setStatus(Status.SUCCESS).
          addAllFileInfo(fileInfos).
          build();

      responseMessage = Message.newBuilder().
          setType(Message.Type.LOCAL_SEARCH_RESPONSE).
          setLocalSearchResponse(localSearchResponse).
          build();
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return responseMessage;
  }

}
