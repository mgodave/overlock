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
class SpinLock {
  val writer = new AtomicBoolean(false)
  val readerCount = new AtomicInteger(0)
  
  /**
   * hold a counter open while performing a thunk
   */
  def readLock[A](op: => A) : A = {

    // this implementation is not necessarily fair since it gives preference
    // to write locks and may cause starvation for read locks, however, it 
    // is at least correct.
    var readLockAcquired = false
    while (!readLockAcquired) {
      while (writer.get) // wait for writers to vacate lock
      readerCount.incrementAndGet // express interest in readlock
      if (writer.get) { // if writer got ther first, back down
        readerCount.decrementAndGet
      } else {
        readLockAcquired = true
      }
    }

    try {
      op
    }
    finally {
      readerCount.getAndDecrement
    }
  }
  
  def writeLock[A](op : => A) : A = {

    // TTAS 
    var writeLockAcquired = false
    while (!writeLockAcquired) {
      while (writer.get || !writer.compareAndSet(false, true)) {}
      writeLockAcquired = true
    }

    while(readerCount.get > 0) {}  //wait for all of the readers to clear

    try {
      op
    } finally {
      writer.set(false)
    }
  }
}
