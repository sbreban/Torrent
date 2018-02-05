package handlers;

import com.google.protobuf.ByteString;
import node.*;
import util.MessageUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReplicateRequestHandler {

  private static final Logger logger = Logger.getLogger(ReplicateRequestHandler.class.getName());

  public static Message handleReplicateRequest(Message message, List<NodeConfiguration> otherNodes, Map<ByteString, List<byte[]>> localFiles, Map<String, ByteString> fileNameToHash) {
    ReplicateResponse.Builder builder = ReplicateResponse.newBuilder();
    ReplicateRequest replicateRequest = message.getReplicateRequest();
    try {
      FileInfo fileInfo = replicateRequest.getFileInfo();
      String filename = fileInfo.getFilename();
      ByteString fileHash = fileInfo.getHash();

      boolean done = false;
      if (filename.isEmpty()) {
        builder.setStatus(Status.MESSAGE_ERROR);
        done = true;
      } else if (fileNameToHash.containsKey(filename) || localFiles.containsKey(fileHash)) {
        builder.setStatus(Status.SUCCESS);
        done = true;
      }

      if (!done) {
        List<ChunkInfo> chunkInfoList = fileInfo.getChunksList();
        int lastValidNodeIndex = 0;
        MessageDigest md = MessageDigest.getInstance("MD5");

        for (ChunkInfo chunkInfo : chunkInfoList) {
          int nodeIndex = lastValidNodeIndex;
          NodeConfiguration otherNode = otherNodes.get(nodeIndex);
          Socket socket = new Socket(InetAddress.getByName(otherNode.getAddr()), otherNode.getPort());

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
              MessageUtil.sendMessage(socket, chunkRequestMessage);
              byte[] buffer = MessageUtil.getMessageBytes(socket);
              if (buffer != null) {
                ChunkResponse chunkResponseMessage = Message.parseFrom(buffer).getChunkResponse();
                if (chunkResponseMessage.getStatus().equals(Status.SUCCESS)) {
                  lastValidNodeIndex = nodeIndex;
                  byte[] digest;
                  byte[] chunkData = chunkResponseMessage.getData().toByteArray();
                  md.update(chunkData);
                  digest = md.digest();
                  if (!chunkInfo.getHash().equals(ByteString.copyFrom(digest)) || chunkData.length != chunkInfo.getSize()) {
                    logger.severe("Invalid chunk hash or chunk data size");
                    throw new IllegalArgumentException();
                  }
                  List<byte[]> fileContent = localFiles.computeIfAbsent(fileHash, k -> new LinkedList<>());
                  fileContent.add(chunkData);
                  fileNameToHash.put(filename, fileHash);
                  logger.fine(chunkResponseMessage.toString());
                } else if (chunkResponseMessage.getStatus().equals(Status.UNABLE_TO_COMPLETE)) {
                  logger.fine("Unable to get chunk " + chunkInfo + " from node " + otherNode);
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
              nodeIndex++;
              socket.close();
              otherNode = otherNodes.get(nodeIndex);
              socket = new Socket(InetAddress.getByName(otherNode.getAddr()), otherNode.getPort());
            }
          }

          socket.close();

          if (nodeIndex == otherNodes.size()) {
            builder.setStatus(Status.UNABLE_TO_COMPLETE);
            break;
          }
        }
      }
    } catch (IOException e) {
      builder.setStatus(Status.UNABLE_TO_COMPLETE);
      logger.log(Level.SEVERE, e.getMessage(), e);
    } catch (NoSuchAlgorithmException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }

    if (builder.getNodeStatusListBuilderList().size() != replicateRequest.getFileInfo().getChunksList().size()) {
      builder.setStatus(Status.UNABLE_TO_COMPLETE);
    }

    return Message.newBuilder().
        setType(Message.Type.REPLICATE_RESPONSE).
        setReplicateResponse(builder.build()).
        build();
  }

}
