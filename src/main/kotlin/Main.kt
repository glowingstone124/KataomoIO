package ind.glowingstone
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.system.measureNanoTime

fun main() {
    // 增加队列大小以处理更高的并发
    val ioUring = ioring_instance(81920, 81920, 8192)
    val path = Path.of("test.txt")
    val readSize = 1024L
    val iterations = 100
    val concurrency = 16
    val ioUringDuration = measureNanoTime {
        repeat(iterations) {
            repeat(concurrency) {
                val deferredRead = ioUring.insert(submission_struct(OPERATION.READ, path, readSize))
            }
        }
    }

    val ioUringAvgDuration = ioUringDuration / iterations
    val ioUringThroughput = (iterations * readSize) / (ioUringDuration / 1_000_000_000.0)

    println("kotlin double ring 平均读取时间: $ioUringAvgDuration 纳秒")
    println("kotlin double ring 吞吐量: $ioUringThroughput 字节/秒")

    val traditionalDuration = measureNanoTime {
        repeat(iterations) {
            repeat(concurrency) {
                thread {
                    val data = java.nio.file.Files.readAllBytes(path)
                }
            }
        }
    }

    val traditionalAvgDuration = traditionalDuration / iterations
    val traditionalThroughput = (iterations * path.toFile().length()) / (traditionalDuration / 1_000_000_000.0)

    println("传统文件读取平均时间: $traditionalAvgDuration 纳秒")
    println("传统文件读取吞吐量: $traditionalThroughput 字节/秒")
}
