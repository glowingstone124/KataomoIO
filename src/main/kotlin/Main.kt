package ind.glowingstone

import java.nio.file.Path

fun main() {
    val ioUring = ioring_instance(12, 12, 8192)

    val deferredRead = ioUring.insert(submission_struct(OPERATION.READ, Path.of("test.txt"), 1))

    while (true) {
        ioUring.completion_queue_ring.forEach {
            if (it.fd == deferredRead && it.code == 0) println(String(it.result as ByteArray))
            return
        }
    }
}

