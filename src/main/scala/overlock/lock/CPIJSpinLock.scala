package overlock.lock

import java.util.concurrent.AtomicBoolean

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
