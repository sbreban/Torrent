package util;

import com.google.protobuf.ByteString;
import node.ChunkInfo;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class ChunkInfoUtil {

  private static final Logger logger = Logger.getLogger(ChunkInfoUtil.class.getName());

  public static List<ChunkInfo> getChunkInfos(byte[] bytes, MessageDigest md) {
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
    chunkInfos.add(chunkInfo);
    logger.fine("Chunk " + blockCount + ": " + Arrays.toString(range));
    return chunkInfos;
  }

}
