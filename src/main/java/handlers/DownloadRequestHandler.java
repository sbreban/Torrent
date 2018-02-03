package handlers;

import com.google.protobuf.ByteString;
import node.DownloadRequest;
import node.DownloadResponse;
import node.Message;
import node.Status;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DownloadRequestHandler {

  private static final Logger logger = Logger.getLogger(DownloadRequestHandler.class.getName());

  public static Message handleDownloadRequest(Message message, Map<ByteString, List<byte[]>> localFiles) {
    Message responseMessage = null;
    try {
      Status downloadResponseStatus = Status.SUCCESS;

      DownloadRequest downloadRequest = message.getDownloadRequest();
      ByteString fileHash = downloadRequest.getFileHash();
      if (fileHash.toByteArray().length != 16) {
        logger.severe("Invalid file hash");
      }
      List<ByteString> dataList = new LinkedList<>();

      DownloadResponse downloadResponse;
      List<byte[]> fileContent = localFiles.get(fileHash);
      if (fileContent != null && fileContent.size() > 0) {
        for (int i = 0; i < fileContent.size(); i++) {
          byte[] content = fileContent.get(i);
          ByteString data = ByteString.copyFrom(content);
          dataList.add(data);
        }
        downloadResponse = DownloadResponse.newBuilder().
            setStatus(downloadResponseStatus).
            setData(ByteString.copyFrom(dataList)).
            build();
      } else {
        downloadResponseStatus = Status.UNABLE_TO_COMPLETE;
        downloadResponse = DownloadResponse.newBuilder().
            setStatus(downloadResponseStatus).
            build();
      }

      responseMessage = Message.newBuilder().
          setType(Message.Type.DOWNLOAD_RESPONSE).
          setDownloadResponse(downloadResponse).
          build();
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return responseMessage;
  }

}
