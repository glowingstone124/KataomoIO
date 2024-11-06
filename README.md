# KataomoIO

a io_uring like I/O library.

```Kotlin
val ioUring = ioring_instance(12, 12, 8192);
```
Create an ioring instance.

Parameter: `SQE length, CQE length, buffer size(bits)`

```Kotlin
val deferredRead = ioUring.insert(submission_struct(OPERATION.READ, Path.of("test.txt"), 1))

while (true) {
	ioUring.completion_queue_ring.forEach {
		if (it.fd == deferredRead && it.code == 0) println(String(it.result as ByteArray))
        return
	}
}
```
Create an task, get its `fd` and wait for complete.