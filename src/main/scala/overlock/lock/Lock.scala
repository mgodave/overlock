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

  def readLock[T](f : => T) : T = {
    lock.readLock.lock
    try {
      f
    } finally {
      lock.readLock.unlock
    }
  }

  // perform a lock upgrade and downgrade as shown in ReentrantReadWriteLock javadoc
  def upgradeToWriteLock[T](f: => T) : T = {
    lock.readLock.unlock
    lock.writeLock.lock
    try {
      f
    } finally {
      lock.readLock.lock
      lock.writeLock.unlock
    }
  }
  
  def writeLock[T](f : => T) : T = {
    lock.writeLock.lock
    try {
      f
    } finally {
      lock.writeLock.unlock
    }
  }
}
