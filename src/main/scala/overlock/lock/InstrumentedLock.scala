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

  val readLockWaitTimer = metrics.timer("read lock wait times")
  val writeLockWaitTimer = metrics.timer("write lock wait times")

  override def readLock[T](f : => T) : T = {
    readLockWaitTimer.time { lock.readLock.lock }
    hasReadLock.set(true)
    readLockAcquireMeter.mark
    try {
      readLockHoldTimer.time { f }
    } finally {
      lock.readLock.unlock
      hasReadLock.set(false)
    }
  }

  override def writeLock[T](f : => T) : T = {
    if (hasReadLock.get) {
      lock.readLock.unlock
    }
    writeLockWaitTimer.time { lock.writeLock.lock }
    writeLockAcquireMeter.mark
    try {
      writeLockHoldTimer.time { f }
    } finally {
      if (hasReadLock.get) {
        lock.readLock.lock
      }
      lock.writeLock.unlock
    }
  }

}
