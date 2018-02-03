package node;

import com.google.protobuf.ByteString;
import handlers.*;
import util.MessageUtil;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
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
    this.server = new ServerSocket(nodeConfiguration.getPort(), 1, InetAddress.getByName(nodeConfiguration.getAddr()));
    this.otherNodes = otherNodes;
    this.localFiles = new HashMap<>();
    this.fileNameToHash = new HashMap<>();
  }

  private void listen() throws Exception {
    while (true) {
      Socket client = this.server.accept();
      String clientAddress = client.getInetAddress().getHostAddress();
      logger.info("New connection from " + clientAddress);
      executor.submit(() -> handleClient(client));

    }
  }

  private void handleClient(Socket client) {
    try {
      byte[] buffer = MessageUtil.getMessageBytes(client);
      if (buffer != null) {
        Message message = Message.parseFrom(buffer);
        logger.fine(client + " " + message.toString());
        if (message.getType().equals(Message.Type.LOCAL_SEARCH_REQUEST)) {
          logger.info(client + " Local search request");
          lock.lock();
          Message responseMessage = LocalSearchRequestHandler.handleLocalSearchRequest(message, localFiles, fileNameToHash);
          lock.unlock();
          sendResponse(client, responseMessage);
        } else if (message.getType().equals(Message.Type.SEARCH_REQUEST)) {
          logger.info(client + " Search request");
          lock.lock();
          Message responseMessage = SearchRequestHandler.handleSearchRequest(message, localNode, otherNodes, localFiles, fileNameToHash);
          lock.unlock();
          sendResponse(client, responseMessage);
        } else if (message.getType().equals(Message.Type.UPLOAD_REQUEST)) {
          logger.info(client + " Upload request");
          lock.lock();
          Message responseMessage = UploadRequestHandler.handleUploadRequest(message, localFiles, fileNameToHash);
          lock.unlock();
          sendResponse(client, responseMessage);
        } else if (message.getType().equals(Message.Type.REPLICATE_REQUEST)) {
          logger.info(client + " Replicate request");
          lock.lock();
          Message responseMessage = ReplicateRequestHandler.handleReplicateRequest(message, otherNodes, localFiles, fileNameToHash);
          lock.unlock();
          sendResponse(client, responseMessage);
        } else if (message.getType().equals(Message.Type.CHUNK_REQUEST)) {
          logger.info(client + " Chunk request");
          lock.lock();
          Message responseMessage = ChunkRequestHandler.handleChunkRequest(message, localFiles);
          lock.unlock();
          sendResponse(client, responseMessage);
        } else if (message.getType().equals(Message.Type.DOWNLOAD_REQUEST)) {
          logger.info(client + " Download request");
          lock.lock();
          Message responseMessage = DownloadRequestHandler.handleDownloadRequest(message, localFiles);
          lock.unlock();
          sendResponse(client, responseMessage);
        }
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage());
    } finally {
      try {
        client.close();
      } catch (IOException e) {
        logger.log(Level.SEVERE, e.getMessage());
      }
    }
  }

  private void sendResponse(Socket client, Message responseMessage) throws IOException {
    OutputStream outputStream = client.getOutputStream();
    byte[] responseMessageSize = ByteBuffer.allocate(4).putInt(responseMessage.toByteArray().length).array();
    outputStream.write(responseMessageSize);
    outputStream.write(responseMessage.toByteArray());
    outputStream.close();
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
