package handlers;

import node.*;
import util.MessageUtil;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReplicateRequestHandler {

  private static final Logger logger = Logger.getLogger(ReplicateRequestHandler.class.getName());

  public static Message handleReplicateRequest(Message message, List<NodeConfiguration> otherNodes) {
    Message responseMessage = null;
    try {
      ReplicateRequest replicateRequest = message.getReplicateRequest();
      FileInfo fileInfo = replicateRequest.getFileInfo();
      List<ChunkInfo> chunkInfoList = fileInfo.getChunksList();
      List<NodeReplicationStatus> nodeReplicationStatuses = new ArrayList<>();
      Status replicateResponseStatus = Status.SUCCESS;
      for (ChunkInfo chunkInfo : chunkInfoList) {
        ChunkRequest chunkRequest = ChunkRequest.newBuilder().
            setFileHash(fileInfo.getHash()).
            setChunkIndex(chunkInfo.getIndex()).
            build();
        Message chunkRequestMessage = Message.newBuilder().
            setType(Message.Type.CHUNK_REQUEST).
            setChunkRequest(chunkRequest).
            build();
        NodeConfiguration otherNode = otherNodes.get(0);
        Socket socket = new Socket(InetAddress.getByName(otherNode.getAddr()), otherNode.getPort());
        OutputStream outputStream = socket.getOutputStream();
        byte[] chunkRequestMessageSize = ByteBuffer.allocate(4).putInt(chunkRequestMessage.toByteArray().length).array();
        outputStream.write(chunkRequestMessageSize);
        outputStream.write(chunkRequestMessage.toByteArray());
        byte[] buffer = MessageUtil.getMessageBytes(socket);
        if (buffer != null) {
          ChunkResponse chunkResponseMessage = Message.parseFrom(buffer).getChunkResponse();
          if (chunkResponseMessage.getStatus().equals(Status.SUCCESS)) {
            Node node = Node.newBuilder().setPort(otherNode.getPort()).setHost(otherNode.getAddr()).build();
            NodeReplicationStatus nodeReplicationStatus = NodeReplicationStatus.newBuilder().
                setNode(node).
                setChunkIndex(chunkRequest.getChunkIndex()).
                setStatus(Status.SUCCESS).
                build();
            nodeReplicationStatuses.add(nodeReplicationStatus);
            logger.fine(chunkResponseMessage.toString());
          } else if (chunkResponseMessage.getStatus().equals(Status.UNABLE_TO_COMPLETE)) {

          }
        }
      }
      ReplicateResponse replicateResponse = ReplicateResponse.newBuilder().
          setStatus(replicateResponseStatus).
          addAllNodeStatusList(nodeReplicationStatuses).
          build();
      responseMessage = Message.newBuilder().
          setType(Message.Type.REPLICATE_RESPONSE).
          setReplicateResponse(replicateResponse).
          build();
      logger.fine(fileInfo.toString());
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage());
    }
    return responseMessage;
  }

}
