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

	private fun read_bytes(channel: ReadableByteChannel, fileSize: Long? = null): ByteArray {
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

		return byteArrayList.toByteArray()
	}

	private fun write_bytes(channel: WritableByteChannel, data: ByteArray?) {
		if (data == null || data.isEmpty()) return

		val buffer = ByteBuffer.allocateDirect(buffer_size)
		var offset = 0
		while (offset < data.size) {
			val chunkSize = min(buffer_size, data.size - offset)
			buffer.clear()
			buffer.put(data, offset, chunkSize)
			buffer.flip()

			channel.write(buffer)
			offset += chunkSize
		}
	}

	fun read_file(path: Path): ByteArray {
		val file = path.toFile()
		val fileSize = file.length()

		if (fileSize <= 0) {
			throw IllegalArgumentException("File is empty or invalid.")
		}
		val channel = FileChannel.open(path, StandardOpenOption.READ)
		return read_bytes(channel, fileSize)
	}

	fun write_file(path: Path, data: ByteArray?) {
		if (data == null || data.isEmpty()) return

		Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { channel ->
			write_bytes(channel, data)
		}
	}

	fun read_socket(socketChannel: SocketChannel): ByteArray {
		return read_bytes(socketChannel)
	}

	fun write_socket(socketChannel: SocketChannel, data: ByteArray?) {
		if (data == null || data.isEmpty()) return

		write_bytes(socketChannel, data)
	}
}
