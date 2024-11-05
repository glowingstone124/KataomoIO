# KataomoIO

a io_uring like I/O library.

```Kotlin
val ioUring = ioring_instance(12, 12, 8192);
```
Create an ioring instance.

Parameter: `SQE length, CQE length, buffer size(bits)`

```Kotlin
val token = ioUring.exec_now(submission_struct(OPERATION.WRITE, Path.of("test.txt"), 0, "Hello, world!".toByteArray()))
thread {
	var result: completion_struct? = null
	do {
		result = ioUring.poll(token)
	} while (result == null)
	println("Write task completed: ${result.code}")
}.join()
```
To create an instant-execute task. Using do-while loop to poll result.

```Kotlin
val readToken = ioUring.submit(submission_struct(OPERATION.READ, Path.of("example.txt"), 2))

ioUring.exec_job(OPERATION.READ, Path.of("example.txt"), readToken)

thread {
	do {
		read_result = ioUring.poll(readToken)
	} while (read_result == null)
	println("Read task completed: ${read_result?.result}")
}.join()
```
Manually start a task and poll result.