package overlock.lock

import com.yammer.metrics.Instrumented

class InstrumentedLock extends Lock with Instrumented {

  val queueGuage = metrics.gauge("queue length") { lock.getQueueLength }
  val readHoldGuage = metrics.gauge("read hold count") { lock.getReadHoldCount }
  val readLockGuage = metrics.gauge("read lock count") { lock.getReadLockCount }
  val writeHoldGuage = metrics.gauge("write hold count") { lock.getWriteHoldCount }

  val readLockAquireMeter = metrics.meter("read lock aquisition rate", "aquisitions")
  val writeLockAquireMeter = metrics.meter("write lock aquisition rate", "aquisitions")

  val readLockHoldTimer = metrics.timer("read lock hold time")
  val writeLockHoldTimer = metrics.timer("write lock hold time")

}
