package ind.glowingstone
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.math.min

class file_operations(val buffer_size: Int = 8192) {

	private fun read_bytes(channel: ReadableByteChannel, fileSize: Long? = null): Pair<ByteArray?, Int> {
		val byteArrayList = mutableListOf<Byte>()
		var remaining = fileSize ?: Long.MAX_VALUE
		val buffer = ByteBuffer.allocateDirect(buffer_size)

		while (remaining > 0) {
			buffer.clear()
			val bytesRead = channel.read(buffer)

			if (bytesRead == -1) break

			buffer.flip()
			val byteArray = ByteArray(bytesRead)
			buffer.get(byteArray)
			byteArrayList.addAll(byteArray.toList())

			remaining -= bytesRead.toLong()
		}

		return if (byteArrayList.isEmpty()) {
			Pair(null, 1)
		} else {
			Pair(byteArrayList.toByteArray(), 0)
		}
	}

	private fun write_bytes(channel: WritableByteChannel, data: ByteArray?): Int {
		if (data == null || data.isEmpty()) return 1

		val buffer = ByteBuffer.allocateDirect(buffer_size)
		var offset = 0
		while (offset < data.size) {
			val chunkSize = min(buffer_size, data.size - offset)
			buffer.clear()
			buffer.put(data, offset, chunkSize)
			buffer.flip()

			val bytesWritten = channel.write(buffer)
			if (bytesWritten == -1) return 2
			offset += chunkSize
		}
		return 0
	}

	fun read_file(path: Path): Pair<ByteArray?, Int> {
		val file = path.toFile()
		val fileSize = file.length()

		if (fileSize <= 0) {
			return null to 1
		}

		val channel = FileChannel.open(path, StandardOpenOption.READ)
		val (data, code) = read_bytes(channel, fileSize)

		return data to code
	}

	fun write_file(path: Path, data: ByteArray?, operation: StandardOpenOption): Pair<ByteArray?, Int> {
		val code = if (data == null || data.isEmpty()) {
			1
		} else {
			val options = if (operation == StandardOpenOption.APPEND) {
				arrayOf(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
			} else {
				arrayOf(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
			}

			Files.newByteChannel(path, *options).use { channel ->
				write_bytes(channel, data)
			}
			0
		}
		return null to code
	}

	fun read_socket(socketChannel: SocketChannel): Pair<ByteArray?, Int> {
		val (data, code) = read_bytes(socketChannel)
		return data to code
	}

	fun write_socket(socketChannel: SocketChannel, data: ByteArray?): Pair<ByteArray?, Int> {
		val code = write_bytes(socketChannel, data)
		return null to code
	}
}
