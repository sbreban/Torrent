package node;

import com.google.protobuf.ByteString;
import handlers.*;
import util.MessageUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TorrentNode {

  private static final Logger logger = Logger.getLogger(TorrentNode.class.getName());

  private ServerSocket server;

  private static final String confFile = "torrent.conf";
  private static final String ipPrefixKey = "ip-prefix";
  private static final String portBaseKey = "port-base";
  private static final String ipSuffixesKey = "ip-suffixes";
  private static final String portOffsetsKey = "port-offsets";

  private NodeConfiguration localNode;
  private List<NodeConfiguration> otherNodes;
  private Map<ByteString, List<byte[]>> localFiles;
  private Map<String, ByteString> fileNameToHash;

  private ExecutorService executor = Executors.newFixedThreadPool(5);
  private ReentrantLock lock = new ReentrantLock();

  public TorrentNode(NodeConfiguration nodeConfiguration, List<NodeConfiguration> otherNodes) throws Exception {
    this.localNode = nodeConfiguration;
    this.server = new ServerSocket(nodeConfiguration.getPort(), 100, InetAddress.getByName(nodeConfiguration.getAddr()));
    this.otherNodes = otherNodes;
    this.localFiles = new HashMap<>();
    this.fileNameToHash = new HashMap<>();
  }

  private void listen() throws Exception {
    while (true) {
      logger.info("Waiting for connection");
      Socket clientSocket = this.server.accept();
      executor.submit(() -> handleClient(clientSocket));
    }
  }

  private void handleClient(Socket clientSocket) {
    String clientAddress = clientSocket.getInetAddress().getHostAddress();
    logger.info("New connection from " + clientAddress);
    try {
      byte[] buffer = MessageUtil.getMessageBytes(clientSocket);
      if (buffer != null) {
        Message message = Message.parseFrom(buffer);
        Message responseMessage = null;
        logger.fine(clientSocket + " " + message.toString());
        if (message.getType().equals(Message.Type.LOCAL_SEARCH_REQUEST)) {
          logger.info(clientSocket + " Local search request");
          lock.lock();
          responseMessage = LocalSearchRequestHandler.handleLocalSearchRequest(message, localFiles, fileNameToHash);
          lock.unlock();
        } else if (message.getType().equals(Message.Type.SEARCH_REQUEST)) {
          logger.info(clientSocket + " Search request");
          lock.lock();
          responseMessage = SearchRequestHandler.handleSearchRequest(message, localNode, otherNodes, localFiles, fileNameToHash);
          lock.unlock();
        } else if (message.getType().equals(Message.Type.UPLOAD_REQUEST)) {
          logger.info(clientSocket + " Upload request");
          lock.lock();
          responseMessage = UploadRequestHandler.handleUploadRequest(message, localFiles, fileNameToHash);
          lock.unlock();
        } else if (message.getType().equals(Message.Type.REPLICATE_REQUEST)) {
          logger.info(clientSocket + " Replicate request");
          lock.lock();
          responseMessage = ReplicateRequestHandler.handleReplicateRequest(message, otherNodes, localFiles, fileNameToHash);
          lock.unlock();
        } else if (message.getType().equals(Message.Type.CHUNK_REQUEST)) {
          logger.info(clientSocket + " Chunk request");
          lock.lock();
          responseMessage = ChunkRequestHandler.handleChunkRequest(message, localFiles);
          lock.unlock();
        } else if (message.getType().equals(Message.Type.DOWNLOAD_REQUEST)) {
          logger.info(clientSocket + " Download request");
          lock.lock();
          responseMessage = DownloadRequestHandler.handleDownloadRequest(message, localFiles);
          lock.unlock();
        }
        if (responseMessage != null) {
          MessageUtil.sendMessage(clientSocket, responseMessage);
        }
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage());
    } finally {
      try {
        clientSocket.close();
      } catch (IOException e) {
        logger.log(Level.SEVERE, e.getMessage());
      }
    }
  }

  public InetAddress getSocketAddress() {
    return this.server.getInetAddress();
  }

  public int getPort() {
    return this.server.getLocalPort();
  }

  public static void main(String[] args) throws Exception {
    String ipPrefix = "";
    int portBase = 0;
    String[] ipSuffixes = new String[0];
    String[] portOffsets = new String[0];

    try (BufferedReader br = new BufferedReader(new FileReader(confFile))) {
      for (int i = 0; i < 4; i++) {
        String sCurrentLine = br.readLine();
        String[] lineElements = sCurrentLine.split("=");
        String key = lineElements[0];
        String value = lineElements[1];
        if (key.equals(ipPrefixKey)) {
          ipPrefix = value;
        } else if (key.equals(portBaseKey)) {
          portBase = Integer.parseInt(value);
        } else if (key.equals(ipSuffixesKey)) {
          ipSuffixes = value.split(" ");
        } else if (key.equals(portOffsetsKey)) {
          portOffsets = value.split(" ");
        }
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage());
    }

    List<NodeConfiguration> otherNodes = new ArrayList<>();
    for (String ipSuffix : ipSuffixes) {
      for (String portOffset : portOffsets) {
        NodeConfiguration nodeConfiguration = getNodeConfiguration(ipPrefix, portBase, ipSuffix, portOffset);
        otherNodes.add(nodeConfiguration);
      }
    }

    String ipPort = args[0];
    String[] ipAndPort = ipPort.split(":");
    NodeConfiguration currentConfiguration = getNodeConfiguration(ipPrefix, portBase, ipAndPort[0], ipAndPort[1]);
    if (!otherNodes.contains(currentConfiguration)) {
      logger.log(Level.SEVERE, "Invalid node configuration");
    } else {
      otherNodes.remove(currentConfiguration);
      TorrentNode app = new TorrentNode(currentConfiguration, otherNodes);

      logger.info("Running Server: " +
          "Host=" + app.getSocketAddress().getHostAddress() +
          " Port=" + app.getPort());

      app.listen();
    }
  }

  private static NodeConfiguration getNodeConfiguration(String ipPrefix, int portBase, String ipSuffix, String portOffset) {
    String bindAddr = ipPrefix + "." + ipSuffix;
    int port = portBase + Integer.parseInt(portOffset);
    return new NodeConfiguration(bindAddr, port);
  }
}
