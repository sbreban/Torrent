package util;

import com.google.protobuf.ByteString;
import node.ChunkInfo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class ChunkInfoUtil {

  private static final Logger logger = Logger.getLogger(ChunkInfoUtil.class.getName());

  public static List<ChunkInfo> getChunkInfos(byte[] bytes, List<byte[]> fileContent) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] digest;
    List<ChunkInfo> chunkInfos = new ArrayList<>();
    int blockSize = 1024;
    int blockCount = (bytes.length + blockSize - 1) / blockSize;

    byte[] range = null;

    for (int i = 0; i < blockCount - 1; i++) {
      int idx = i * blockSize;
      range = Arrays.copyOfRange(bytes, idx, idx + blockSize);

      md.update(range);
      digest = md.digest();

      ChunkInfo chunkInfo = ChunkInfo.newBuilder().
          setIndex(i).
          setSize(blockSize).
          setHash(ByteString.copyFrom(digest)).
          build();
      fileContent.add(range);
      logger.fine("Chunk " + i + ": " + Arrays.toString(range));
      chunkInfos.add(chunkInfo);
    }

    int end;
    if (bytes.length % blockSize == 0) {
      end = bytes.length;
    } else {
      end = bytes.length % blockSize + blockSize * (blockCount - 1);
    }
    range = Arrays.copyOfRange(bytes, (blockCount - 1) * blockSize, end);

    md.update(range);
    digest = md.digest();

    ChunkInfo chunkInfo = ChunkInfo.newBuilder().
        setIndex(blockCount - 1).
        setSize(end - ((blockCount - 1) * blockSize)).
        setHash(ByteString.copyFrom(digest)).
        build();
    fileContent.add(range);
    logger.fine("Chunk " + blockCount + ": " + Arrays.toString(range));
    chunkInfos.add(chunkInfo);
    return chunkInfos;
  }

  public static List<ChunkInfo> getChunkInfos(List<byte[]> fileContent) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] digest;
    List<ChunkInfo> chunkInfos = new ArrayList<>();

    for (int i = 0; i < fileContent.size(); i++) {
      byte[] content = fileContent.get(i);
      md.update(content);
      digest = md.digest();

      ChunkInfo chunkInfo = ChunkInfo.newBuilder().
          setIndex(i).
          setSize(content.length).
          setHash(ByteString.copyFrom(digest)).
          build();
      logger.fine("Chunk " + chunkInfo.toString());
      chunkInfos.add(chunkInfo);
    }

    return chunkInfos;
  }

}
