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

import java.util.concurrent.locks.ReentrantReadWriteLock

class Lock {
  val lock = new ReentrantReadWriteLock
  val hasReadLock = new ThreadLocal[Boolean]

  hasReadLock.set(false)
  
  def readLock[T](f : => T) : T = {
    lock.readLock.lock
    hasReadLock.set(true)
    try {
      f
    } finally {
      lock.readLock.unlock
      hasReadLock.set(false)
    }
  }
  
  def writeLock[T](f : => T) : T = {
    // if we already have the read lock then we need to upgrade appropriately
    if (hasReadLock.get) {
      lock.readLock.unlock
    }
    lock.writeLock.lock
    try {
      f
    } finally {
      // if we had the read lock before acquiring the write lock, downgrade appropriately
      if (hasReadLock.get) {
        lock.readLock.lock
      }
      lock.writeLock.unlock
    }
  }
}
