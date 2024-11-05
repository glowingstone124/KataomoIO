package ind.glowingstone

import java.nio.file.Path
import kotlin.concurrent.thread


suspend fun main() {
    val ioUring = ioring_instance(12,12,8192);
    val writeToken = ioUring.submit(submission_struct(OPERATION.WRITE, Path.of("example.txt"), 1, "Hello, world!".toByteArray()))

    val readToken = ioUring.submit(submission_struct(OPERATION.READ, Path.of("example.txt"), 2))

    ioUring.exec_job(OPERATION.WRITE, Path.of("example.txt"))

    ioUring.exec_job(OPERATION.READ, Path.of("example.txt"))

    var write_result: completion_struct? = null;
    var read_result: completion_struct? = null;

    thread {
        do {
            write_result = ioUring.poll(writeToken)
        } while (write_result == null)
        println("Write task completed: ${write_result?.result}")
    }.join()

    thread {
        do {
            read_result = ioUring.poll(readToken)
        } while (read_result == null)
        println("Read task completed: ${read_result?.result}")
    }.join()
}

