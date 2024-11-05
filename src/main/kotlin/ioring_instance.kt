package ind.glowingstone

import ind.glowingstone.Exceptions.QueueOverFlowException
import kotlinx.coroutines.*
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue

val coroutineScope = Dispatchers.IO + SupervisorJob()

class ioring_instance(
    val sqe_length: Int = 4,
    val cqe_length: Int = 4,
    val buffer_size: Int = 8192
) {

    val submission_queue_ring = ConcurrentLinkedQueue<submission_struct>()
    val completion_queue_ring = ConcurrentLinkedQueue<completion_struct>()
    val fop = file_operations(buffer_size)
    val taskMap = mutableMapOf<String, CompletableDeferred<completion_struct>>()

    fun submit(sq: submission_struct): String {
        if (submission_queue_ring.size >= sqe_length) throw QueueOverFlowException()
        val token = sq.token
        submission_queue_ring.add(sq)
        return token
    }

    fun complete(cq: completion_struct) {
        if (completion_queue_ring.size >= cqe_length) throw QueueOverFlowException()
        completion_queue_ring.add(cq)
    }

    suspend fun exec_job(operation: OPERATION, path: Path) {
        withContext(coroutineScope) {
            while (submission_queue_ring.isNotEmpty()) {
                val sq = submission_queue_ring.poll()
                when (operation) {
                    OPERATION.READ -> {
                        val content = fop.read_bytes(path)
                        val completion = completion_struct(content, 0)
                        taskMap[sq.token]?.complete(completion)
                        complete(completion)
                    }

                    OPERATION.WRITE -> {
                        fop.write_bytes(path, sq.data as ByteArray)
                        val completion = completion_struct(null, 0)
                        taskMap[sq.token]?.complete(completion)
                        complete(completion)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun poll(token: String): completion_struct? {
        val deferred = taskMap[token]
        return if (deferred != null && deferred.isCompleted) {
            taskMap.remove(token)
            deferred.getCompleted()
        } else {
            null
        }
    }
}

class submission_struct(
    val operation: OPERATION,
    val path: Path,
    val priority: Long,
    val data: ByteArray? = null,
    val token: String = generateToken()
)

class completion_struct(val result: Any?, val code: Int)

enum class OPERATION {
    READ,
    WRITE
}

fun generateToken(): String {
    return java.util.UUID.randomUUID().toString()
}