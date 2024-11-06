package ind.glowingstone

import ind.glowingstone.Exceptions.FileTargetNotSupportException
import ind.glowingstone.Exceptions.QueueOverFlowException
import kotlinx.coroutines.*
import java.nio.channels.SocketChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random

val coroutineScope = Dispatchers.IO + SupervisorJob()

class ioring_instance(
	val sqe_length: Int = 4,
	val cqe_length: Int = 4,
	val buffer_size: Int = 8192
) {
	val submission_queue_ring = ConcurrentLinkedQueue<submission_struct>()
	val completion_queue_ring = ConcurrentLinkedQueue<completion_struct>()
	val fop = file_operations(buffer_size)

	init {
		startPolling()
	}

	fun insert(input: submission_struct): Long {
		if (submission_queue_ring.size >= sqe_length) throw QueueOverFlowException()
		submission_queue_ring.add(input)
		return input.td
	}

	fun poll(): completion_struct? {
		return completion_queue_ring.poll()
	}

	@OptIn(DelicateCoroutinesApi::class)
	private fun startPolling() {
		GlobalScope.launch(coroutineScope) {
			while (true) {
				delay(100)
				execute()
			}
		}
	}

	fun execute() {
		submission_queue_ring.forEach {
			var result: Pair<ByteArray?, Int>? = null
			when (it.operation) {
				OPERATION.READ -> {
					result = if (it.path is Path) {
						fop.read_file(path = it.path)
					} else if (it.path is SocketChannel) {
						fop.read_socket(it.path)
					} else {
						throw FileTargetNotSupportException()
					}
				}
				OPERATION.WRITE -> {
					if (it.path is Path) {
						fop.write_file(path = it.path, it.data, StandardOpenOption.APPEND)
					} else if (it.path is SocketChannel) {
						fop.write_socket(it.path, it.data)
					} else {
						throw FileTargetNotSupportException()
					}
				}
				OPERATION.WRITE_OVERRIDE -> {
					if (it.path is Path) {
						fop.write_file(path = it.path, it.data, StandardOpenOption.TRUNCATE_EXISTING)
					} else if (it.path is SocketChannel) {
						fop.write_socket(it.path, it.data)
					} else {
						throw FileTargetNotSupportException()
					}
				}
			}
			if (result != null) {
				completion_queue_ring.add(completion_struct(result = result.first, code = result.second, fd = it.td))
			}
			if (result == null) {
				completion_queue_ring.add(completion_struct(null, 1, fd = it.td))
			}
		}
	}
}

class submission_struct(
	val operation: OPERATION,
	val path: Any,
	val priority: Long,
	val data: ByteArray? = null,
	val td: Long = randomLong()
)

class completion_struct(val result: Any?, val code: Int, val fd: Long)

enum class OPERATION {
	READ,
	WRITE,
	WRITE_OVERRIDE,
}

fun randomLong(): Long {
	return Random.nextLong(25566, Long.MAX_VALUE)
}
