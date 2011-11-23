package overlock.lock

import java.util.concurrent.TimeUnit
import com.yammer.metrics.Instrumented

class InstrumentedLock extends Lock with Instrumented {

  val queueGuage = metrics.gauge("queue length") { lock.getQueueLength }
  val readHoldGuage = metrics.gauge("read hold count") { lock.getReadHoldCount }
  val readLockGuage = metrics.gauge("read lock count") { lock.getReadLockCount }
  val writeHoldGuage = metrics.gauge("write hold count") { lock.getWriteHoldCount }

  val readLockAcquireMeter = metrics.meter("read lock aquisition rate", "aquisitions")
  val writeLockAcquireMeter = metrics.meter("write lock aquisition rate", "aquisitions")

  val readLockHoldTimer = metrics.timer("read lock hold times")
  val writeLockHoldTimer = metrics.timer("write lock hold times")

  override def readLock[T](f : => T) : T = {
    lock.readLock.lock
    readLockAcquireMeter.mark
    val startTime = System.nanoTime
    try {
      f
    } finally {
      lock.readLock.unlock
      readLockHoldTimer.update(System.nanoTime - startTime, TimeUnit.NANOSECONDS)
    }
  }

  override def writeLock[T](f : => T) : T = {
    lock.writeLock.lock
    writeLockAcquireMeter.mark
    val startTime = System.nanoTime
    try {
      f
    } finally {
      lock.writeLock.unlock
      writeLockHoldTimer.update(System.nanoTime - startTime, TimeUnit.NANOSECONDS)
    }
  }

}
