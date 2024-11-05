package ind.glowingstone

import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlinx.coroutines.*

suspend fun main() {
    val ioUring = ioring_instance(12, 12, 8192);
    val socketChannel = SocketChannel.open()
    socketChannel.connect(InetSocketAddress("127.0.0.1", 8080))

    val writeToken =
        ioUring.submit(submission_struct(OPERATION.WRITE, Path.of("example.txt"), 1, "Hello, world!".toByteArray()))

    val readToken = ioUring.submit(submission_struct(OPERATION.READ, Path.of("example.txt"), 2))

    val socket_writeToken =
        ioUring.submit(submission_struct(OPERATION.WRITE, socketChannel, 1, "Hello, world!".toByteArray()))

    val socket_readToken = ioUring.submit(submission_struct(OPERATION.READ, socketChannel, 2))

    ioUring.exec_job(OPERATION.READ, socketChannel, socket_readToken)

    ioUring.exec_job(OPERATION.WRITE, socketChannel, socket_writeToken)

    ioUring.exec_job(OPERATION.READ, Path.of("example.txt"), readToken)

    ioUring.exec_job(OPERATION.WRITE, Path.of("example.txt"), writeToken)

    //OR
    val token = ioUring.exec_now(submission_struct(OPERATION.WRITE, Path.of("test.txt"), 0, "Hello, world!".toByteArray()))
    thread {
        var result: completion_struct? = null
        do {
            result = ioUring.poll(token)
        } while (result == null)
        println("Write task completed: ${result.code}")
    }.join()

    var write_result: completion_struct? = null
    var read_result: completion_struct? = null

    var socket_read_result: completion_struct? = null
    var socket_write_result: completion_struct? = null

    thread {
        do {
            write_result = ioUring.poll(writeToken)
        } while (write_result == null)
        println("Write task completed: ${write_result?.code}")
    }.join()

    thread {
        do {
            read_result = ioUring.poll(readToken)
        } while (read_result == null)
        println("Read task completed: ${read_result?.result}")
    }.join()

    thread {
        do {
            socket_read_result = ioUring.poll(socket_readToken)
        } while (socket_read_result == null)
        println("Socket read task completed: ${socket_read_result?.result}")
    }.join()

    thread {
        do {
            socket_write_result = ioUring.poll(socket_writeToken)
        } while (socket_write_result == null)
        println("Socket write task completed: ${socket_write_result?.code}")
    }.join()
}

