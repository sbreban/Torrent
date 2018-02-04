package handlers;

import com.google.protobuf.ByteString;
import node.DownloadRequest;
import node.DownloadResponse;
import node.Message;
import node.Status;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DownloadRequestHandler {

  private static final Logger logger = Logger.getLogger(DownloadRequestHandler.class.getName());

  public static Message handleDownloadRequest(Message message, Map<ByteString, List<byte[]>> localFiles) {
    DownloadResponse.Builder builder = DownloadResponse.newBuilder();

    DownloadRequest downloadRequest = message.getDownloadRequest();
    ByteString fileHash = downloadRequest.getFileHash();
    if (fileHash.toByteArray().length != 16) {
      logger.severe("Invalid file hash");
      builder.setStatus(Status.MESSAGE_ERROR);
    } else {
      List<ByteString> dataList = new LinkedList<>();

      List<byte[]> fileContent = localFiles.get(fileHash);
      if (fileContent != null && fileContent.size() > 0) {
        for (int i = 0; i < fileContent.size(); i++) {
          byte[] content = fileContent.get(i);
          ByteString data = ByteString.copyFrom(content);
          dataList.add(data);
        }
        builder.setStatus(Status.SUCCESS).
            setData(ByteString.copyFrom(dataList));
      } else {
        builder.setStatus(Status.UNABLE_TO_COMPLETE);
      }
    }

    return Message.newBuilder().
        setType(Message.Type.DOWNLOAD_RESPONSE).
        setDownloadResponse(builder.build()).
        build();
  }

}
