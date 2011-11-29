//
// Copyright 2011, Boundary
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package overlock.lock

import java.util.Random
import java.util.concurrent.atomic._

/**
 * @author Dietrich Featherston
 * @author Cliff Moon
 */
trait SpinLockable {
  val spinlock = new SpinLock
}
 
// TODO: A true spin lock would probably be better implemented using a queue lock
// TODO: I am currently reading ch. 7 of "The Art of Multiprocessor Programming"
//       and refining this algorithm as I go...
// TODO: This particular implementation could be served well by an exponential backoff
//       http://web.mit.edu/6.173/www/currentsemester/readings/R06-scalable-synchronization-1991.pdf
//       this paper seems to suggest that ttas lock with exp backoff scales extremely well.
class SpinLock(minDelay:Int = 2, maxDelay:Int = 10) {
  val writer = new AtomicBoolean(false)
  val readerCount = new AtomicInteger(0)
  val random = new Random
  /**
   * hold a counter open while performing a thunk
   */
  def readLock[A](op: => A) : A = {

    // this implementation is not necessarily fair since it gives preference
    // to write locks and may cause starvation for read locks, however, it 
    // is at least correct.
    
    val limit = minDelay
    var readLockAcquired = false
    while (!readLockAcquired) {
      while (writer.get) // wait for writers to vacate lock
      readerCount.incrementAndGet // express interest in readlock
      if (writer.get) {
        // at this point we don't know whether the increment or the write.set(true)
        // happened first.  If the write.set(true) did happen first then the writer
        // might already be in the critical section and we need to backoff.  Unfortunatly
        // we cannot determine whether the increment happened first (which would stop the 
        // writer at the busy wait on readerCount) so we must backoff and try again.
        readerCount.decrementAndGet
        
        // There is contention on the write lock, exponentially backoff
        val delay = random.nextInt(limit)
        limit = Math.min(maxDelay, limit * 2)
        Thread.sleep(delay)
      } else {
        readLockAcquired = true
      }
    }

    try {
      op
    }
    finally {
      readerCount.decrementAndGet
    }
  }
  
  def writeLock[A](op : => A) : A = {

    // TTAS 
    var writeLockAcquired = false
    var limit = minDelay
    while (!writeLockAcquired) {
      while (writer.get) {}
      if (!writer.compareAndSet(false, true)) {
        // There is contention on the write lock, exponentially backoff
        val delay = random.nextInt(limit)
        limit = Math.min(maxDelay, limit * 2)
        Thread.sleep(delay)
      } else {
        writeLockAcquired = true
      }
    }

    while(readerCount.get > 0) {}  //wait for all of the readers to clear

    try {
      op
    } finally {
      writer.set(false)
    }
  }
}
