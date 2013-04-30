package spark.storage

import spark.serializer.Serializer


private[spark]
class ShuffleWriterGroup(val id: Int, val writers: Array[BlockObjectWriter])


private[spark]
class ShuffleBlockManager(blockManager: BlockManager) {

  def forShuffle(shuffleId: Int, numBuckets: Int, serializer: Serializer): Shuffle = {
    new Shuffle(shuffleId, numBuckets, serializer)
  }

  class Shuffle(shuffleId: Int, numBuckets: Int, serializer: Serializer) {

    // Get a group of writers for a map task.
    def acquireWriters(mapId: Int): ShuffleWriterGroup = {
      val bufferSize = System.getProperty("spark.shuffle.file.buffer.kb", "100").toInt * 1024
      val writers = Array.tabulate[BlockObjectWriter](numBuckets) { bucketId =>
        val blockId = ShuffleBlockManager.blockId(shuffleId, bucketId, mapId)
        blockManager.getDiskBlockWriter(blockId, serializer, bufferSize).open()
      }
      new ShuffleWriterGroup(mapId, writers)
    }

    def releaseWriters(group: ShuffleWriterGroup) = {
      // Nothing really to release here.
    }
  }
}


private[spark]
object ShuffleBlockManager {

  // Returns the block id for a given shuffle block.
  def blockId(shuffleId: Int, bucketId: Int, groupId: Int): String = {
    "shuffle_" + shuffleId + "_" + groupId + "_" + bucketId
  }

  // Returns true if the block is a shuffle block.
  def isShuffle(blockId: String): Boolean = blockId.startsWith("shuffle_")
}
