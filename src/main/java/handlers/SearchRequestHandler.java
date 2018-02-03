package handlers;

import com.google.protobuf.ByteString;
import node.*;
import util.MessageUtil;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SearchRequestHandler {

  private static final Logger logger = Logger.getLogger(SearchRequestHandler.class.getName());

  public static Message handleSearchRequest(Message message, NodeConfiguration localNode, List<NodeConfiguration> otherNodes, Map<ByteString, List<byte[]>> localFiles, Map<String, ByteString> fileNameToHash) {
    Message responseMessage = null;
    try {
      SearchRequest searchRequest = message.getSearchRequest();
      String regex = searchRequest.getRegex();

      LocalSearchRequest localSearchRequest = LocalSearchRequest.newBuilder().
          setRegex(regex).
          build();
      Message localSearchRequestMessage = Message.newBuilder().
          setType(Message.Type.LOCAL_SEARCH_REQUEST).
          setLocalSearchRequest(localSearchRequest).
          build();

      List<NodeSearchResult> nodeSearchResults = new ArrayList<>();

      for (int nodeIndex = 0; nodeIndex < otherNodes.size(); nodeIndex++) {
        NodeConfiguration otherNode = otherNodes.get(nodeIndex);

        Socket socket = new Socket(InetAddress.getByName(otherNode.getAddr()), otherNode.getPort());
        OutputStream outputStream = socket.getOutputStream();
        byte[] chunkRequestMessageSize = ByteBuffer.allocate(4).putInt(localSearchRequestMessage.toByteArray().length).array();
        outputStream.write(chunkRequestMessageSize);
        outputStream.write(localSearchRequestMessage.toByteArray());
        byte[] buffer = MessageUtil.getMessageBytes(socket);
        if (buffer != null) {
          LocalSearchResponse localSearchResponse = Message.parseFrom(buffer).getLocalSearchResponse();
          if (localSearchResponse.getStatus().equals(Status.SUCCESS)) {
            addSearchResult(otherNode, nodeSearchResults, localSearchResponse);
          } else if (localSearchResponse.getStatus().equals(Status.UNABLE_TO_COMPLETE)) {

          }
        }
      }

      Message localSearchResponseMessage = LocalSearchRequestHandler.handleLocalSearchRequest(localSearchRequestMessage, localFiles, fileNameToHash);
      LocalSearchResponse localSearchResponse = localSearchResponseMessage.getLocalSearchResponse();
      if (localSearchResponse.getStatus().equals(Status.SUCCESS)) {
        addSearchResult(localNode, nodeSearchResults, localSearchResponse);
      }

      SearchResponse searchResponse = SearchResponse.newBuilder().
          setStatus(Status.SUCCESS).
          addAllResults(nodeSearchResults).
          build();

      responseMessage = Message.newBuilder().
          setType(Message.Type.SEARCH_RESPONSE).
          setSearchResponse(searchResponse).
          build();
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return responseMessage;
  }

  private static void addSearchResult(NodeConfiguration nodeConfiguration, List<NodeSearchResult> nodeSearchResults, LocalSearchResponse localSearchResponse) {
    Node node = Node.newBuilder().setPort(nodeConfiguration.getPort()).setHost(nodeConfiguration.getAddr()).build();
    List<FileInfo> fileInfos = localSearchResponse.getFileInfoList();
    logger.fine("Found " + fileInfos.toString() + " on " + node);
    NodeSearchResult nodeSearchResult = NodeSearchResult.newBuilder().
        setNode(node).
        setStatus(Status.SUCCESS).
        addAllFiles(fileInfos).
        build();
    nodeSearchResults.add(nodeSearchResult);
  }

}
