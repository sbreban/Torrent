package handlers;

import com.google.protobuf.ByteString;
import node.*;
import util.MessageUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReplicateRequestHandler {

  private static final Logger logger = Logger.getLogger(ReplicateRequestHandler.class.getName());

  public static Message handleReplicateRequest(Message message, List<NodeConfiguration> otherNodes, Map<ByteString, List<byte[]>> localFiles, Map<String, ByteString> fileNameToHash) {
    ReplicateResponse.Builder builder = ReplicateResponse.newBuilder();
    try {
      ReplicateRequest replicateRequest = message.getReplicateRequest();
      FileInfo fileInfo = replicateRequest.getFileInfo();
      String filename = fileInfo.getFilename();
      ByteString fileHash = fileInfo.getHash();
      if (fileNameToHash.containsKey(filename) || localFiles.containsKey(fileHash)) {
        builder.setStatus(Status.SUCCESS);
      }
      List<ChunkInfo> chunkInfoList = fileInfo.getChunksList();

      for (ChunkInfo chunkInfo : chunkInfoList) {
        int nodeIndex = 0;

        ChunkRequest chunkRequest = ChunkRequest.newBuilder().
            setFileHash(fileHash).
            setChunkIndex(chunkInfo.getIndex()).
            build();
        Message chunkRequestMessage = Message.newBuilder().
            setType(Message.Type.CHUNK_REQUEST).
            setChunkRequest(chunkRequest).
            build();

        while (nodeIndex < otherNodes.size()) {
          try {
            NodeConfiguration otherNode = otherNodes.get(nodeIndex);
            Socket socket = new Socket(InetAddress.getByName(otherNode.getAddr()), otherNode.getPort());
            OutputStream outputStream = socket.getOutputStream();
            byte[] chunkRequestMessageSize = ByteBuffer.allocate(4).putInt(chunkRequestMessage.toByteArray().length).array();
            outputStream.write(chunkRequestMessageSize);
            outputStream.write(chunkRequestMessage.toByteArray());
            byte[] buffer = MessageUtil.getMessageBytes(socket);
            if (buffer != null) {
              ChunkResponse chunkResponseMessage = Message.parseFrom(buffer).getChunkResponse();
              if (chunkResponseMessage.getStatus().equals(Status.SUCCESS)) {
                List<byte[]> fileContent = localFiles.computeIfAbsent(fileHash, k -> new LinkedList<>());
                fileContent.add(chunkResponseMessage.getData().toByteArray());
                fileNameToHash.put(filename, fileHash);
                logger.fine(chunkResponseMessage.toString());
              } else if (chunkResponseMessage.getStatus().equals(Status.UNABLE_TO_COMPLETE)) {
                nodeIndex++;
                throw new IllegalArgumentException();
              }
              Node node = Node.newBuilder().setPort(otherNode.getPort()).setHost(otherNode.getAddr()).build();
              NodeReplicationStatus nodeReplicationStatus = NodeReplicationStatus.newBuilder().
                  setNode(node).
                  setChunkIndex(chunkRequest.getChunkIndex()).
                  setStatus(Status.SUCCESS).
                  build();
              builder.addNodeStatusList(nodeReplicationStatus);
            }

            logger.fine(fileInfo.toString());
            break;
          } catch (IllegalArgumentException e) {
          }
        }

        if (nodeIndex == otherNodes.size()) {
          builder.setStatus(Status.UNABLE_TO_COMPLETE);
        }
      }
    } catch (IOException e) {
      builder.setStatus(Status.UNABLE_TO_COMPLETE);
      logger.log(Level.SEVERE, e.getMessage());
    }
    return Message.newBuilder().
        setType(Message.Type.REPLICATE_RESPONSE).
        setReplicateResponse(builder.build()).
        build();
  }

}
