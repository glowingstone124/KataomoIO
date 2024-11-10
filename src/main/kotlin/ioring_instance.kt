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


	@OptIn(DelicateCoroutinesApi::class)
	private fun startPolling() {
		GlobalScope.launch(coroutineScope) {
			while (true) {
				delay(5)
				execute()
			}
		}
	}


	fun execute() {
		val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
		val jobs = mutableListOf<Job>()

		submission_queue_ring.forEach { submission ->
			jobs += scope.launch {
				var result: Pair<ByteArray?, Int>? = null
				when (submission.operation) {
					OPERATION.READ -> {
						result = if (submission.path is Path) {
							fop.read_file(path = submission.path)
						} else if (submission.path is SocketChannel) {
							fop.read_socket(submission.path)
						} else {
							throw FileTargetNotSupportException()
						}
					}

					OPERATION.WRITE -> {
						if (submission.path is Path) {
							fop.write_file(path = submission.path, submission.data, StandardOpenOption.APPEND)
						} else if (submission.path is SocketChannel) {
							fop.write_socket(submission.path, submission.data)
						} else {
							throw FileTargetNotSupportException()
						}
					}

					OPERATION.WRITE_OVERRIDE -> {
						if (submission.path is Path) {
							fop.write_file(
								path = submission.path,
								submission.data,
								StandardOpenOption.TRUNCATE_EXISTING
							)
						} else if (submission.path is SocketChannel) {
							fop.write_socket(submission.path, submission.data)
						} else {
							throw FileTargetNotSupportException()
						}
					}
				}

				if (completion_queue_ring.size >= cqe_length) throw QueueOverFlowException()

				if (result != null) {
					completion_queue_ring.add(
						completion_struct(
							result = result.first,
							code = result.second,
							fd = submission.td
						)
					)
				} else {
					completion_queue_ring.add(completion_struct(null, 1, fd = submission.td))
				}
			}
		}

		runBlocking {
			jobs.joinAll()
		}
	}

	fun get(td: Long): Any? {
		return completion_queue_ring
			.firstOrNull { it.fd == td && it.code == 0 }
			?.let { completion ->
				completion_queue_ring.remove(completion)
				return completion.result
			}
		return null
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
