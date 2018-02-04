package handlers;

import com.google.protobuf.ByteString;
import node.*;
import util.MessageUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SearchRequestHandler {

  private static final Logger logger = Logger.getLogger(SearchRequestHandler.class.getName());

  public static Message handleSearchRequest(Message message, NodeConfiguration localNode, List<NodeConfiguration> otherNodes, Map<ByteString, List<byte[]>> localFiles, Map<String, ByteString> fileNameToHash) {
    SearchResponse.Builder builder = SearchResponse.newBuilder();

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

      for (int nodeIndex = 0; nodeIndex < otherNodes.size(); nodeIndex++) {
        NodeConfiguration otherNode = otherNodes.get(nodeIndex);

        Socket socket = new Socket(InetAddress.getByName(otherNode.getAddr()), otherNode.getPort());
        MessageUtil.sendMessage(socket, localSearchRequestMessage);
        byte[] buffer = MessageUtil.getMessageBytes(socket);
        if (buffer != null) {
          LocalSearchResponse localSearchResponse = Message.parseFrom(buffer).getLocalSearchResponse();
          if (localSearchResponse.getStatus().equals(Status.SUCCESS)) {
            addSearchResult(otherNode, builder, localSearchResponse);
          } else if (localSearchResponse.getStatus().equals(Status.UNABLE_TO_COMPLETE)) {
            logger.fine("No search result from node " + otherNode);
          }
        }
      }

      Message localSearchResponseMessage = LocalSearchRequestHandler.handleLocalSearchRequest(localSearchRequestMessage, localFiles, fileNameToHash);
      LocalSearchResponse localSearchResponse = localSearchResponseMessage.getLocalSearchResponse();
      if (localSearchResponse.getStatus().equals(Status.SUCCESS)) {
        addSearchResult(localNode, builder, localSearchResponse);
      }

      builder.setStatus(Status.SUCCESS);
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage());
      builder.setStatus(Status.PROCESSING_ERROR);
    }

    return Message.newBuilder().
        setType(Message.Type.SEARCH_RESPONSE).
        setSearchResponse(builder.build()).
        build();
  }

  private static void addSearchResult(NodeConfiguration nodeConfiguration, SearchResponse.Builder builder, LocalSearchResponse localSearchResponse) {
    Node node = Node.newBuilder().setPort(nodeConfiguration.getPort()).setHost(nodeConfiguration.getAddr()).build();
    List<FileInfo> fileInfos = localSearchResponse.getFileInfoList();
    logger.fine("Found " + fileInfos.toString() + " on " + node);
    NodeSearchResult nodeSearchResult = NodeSearchResult.newBuilder().
        setNode(node).
        setStatus(Status.SUCCESS).
        addAllFiles(fileInfos).
        build();
    builder.addResults(nodeSearchResult);
  }

}
