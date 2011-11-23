package overlock.lock

import java.util.concurrent.AtomicBoolean

//TODO an adaptive spinlock might be a fun exercise: http://blogs.oracle.com/dave/resource/MustangSync.pdf
//TODO this should really be implemented with LockSupport
class CPIJSpinLock(spinsBeforeYield = 100, spinsBeforeSleep = 200, sleepTime = 1) {

  val busy = new AtomicBoolean(false)

  def lock = {
    var spins = 0
    while ( true ) {
      if (!busy.get()) {
        if (busy.compareAndSet(false, true)) {
          return
        }
      }

      if (spins < spinsBeforeYield) {
        ++spins
      } else if (spins < spinsBeforeSleep) {
        ++spins
        Thread.yield
      } else {
        Thread.sleep(sleepTime)
        sleepTime = 3 * sleepTime / 2 + 1
      }
    }
  }

  def unlock = {
    if (busy.compareAndSet(true, false)) { }
  }

}
