package ind.glowingstone

import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class file_operations(val buffer_size: Int = 8192) {

	val CHUNK_SIZE: Long = 1L * 1024 * 1024 * 1024  // 1GB

	fun read_bytes(path: Path): ByteArray {
		val file = path.toFile()
		val fileSize = file.length()

		if (fileSize <= 0) {
			throw IllegalArgumentException("File is empty or invalid.")
		}

		val byteArrayList = mutableListOf<Byte>()
		val channel = FileChannel.open(path, StandardOpenOption.READ)

		var position = 0L
		while (position < fileSize) {
			val chunkSize = minOf(CHUNK_SIZE, fileSize - position)
			val mappedByteBuffer: MappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, position, chunkSize)

			val byteArray = ByteArray(chunkSize.toInt())
			mappedByteBuffer.get(byteArray)
			byteArrayList.addAll(byteArray.toList())

			position += chunkSize
		}

		return byteArrayList.toByteArray()
	}

	fun write_bytes(path: Path, data: ByteArray?) {
		if (data == null || data.isEmpty()) return

		Files.newByteChannel(path,
			StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { channel ->
			val buffer = ByteBuffer.allocate(buffer_size)
			var offset = 0
			while (offset < data.size) {
				val chunkSize = minOf(buffer_size, data.size - offset)
				buffer.clear()
				buffer.put(data, offset, chunkSize)
				buffer.flip()

				channel.write(buffer)
				offset += chunkSize
			}
		}
	}

}