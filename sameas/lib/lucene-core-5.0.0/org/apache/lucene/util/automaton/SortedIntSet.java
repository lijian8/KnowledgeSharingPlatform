package org.apache.lucene.util.automaton;

/*
 *
 * Copyright(c) 2015, Samsung Electronics Co., Ltd.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.
    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

import java.util.TreeMap;
import java.util.Map;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.RamUsageEstimator;

// Just holds a set of int[] states, plus a corresponding
// int[] count per state.  Used by
// BasicOperations.determinize
final class SortedIntSet {
  int[] values;
  int[] counts;
  int upto;
  private int hashCode;

  // If we hold more than this many states, we switch from
  // O(N^2) linear ops to O(N log(N)) TreeMap
  private final static int TREE_MAP_CUTOVER = 30;

  private final Map<Integer,Integer> map = new TreeMap<>();

  private boolean useTreeMap;

  int state;

  public SortedIntSet(int capacity) {
    values = new int[capacity];
    counts = new int[capacity];
  }

  // Adds this state to the set
  public void incr(int num) {
    if (useTreeMap) {
      final Integer key = num;
      Integer val = map.get(key);
      if (val == null) {
        map.put(key, 1);
      } else {
        map.put(key, 1+val);
      }
      return;
    }

    if (upto == values.length) {
      values = ArrayUtil.grow(values, 1+upto);
      counts = ArrayUtil.grow(counts, 1+upto);
    }

    for(int i=0;i<upto;i++) {
      if (values[i] == num) {
        counts[i]++;
        return;
      } else if (num < values[i]) {
        // insert here
        int j = upto-1;
        while (j >= i) {
          values[1+j] = values[j];
          counts[1+j] = counts[j];
          j--;
        }
        values[i] = num;
        counts[i] = 1;
        upto++;
        return;
      }
    }

    // append
    values[upto] = num;
    counts[upto] = 1;
    upto++;

    if (upto == TREE_MAP_CUTOVER) {
      useTreeMap = true;
      for(int i=0;i<upto;i++) {
        map.put(values[i], counts[i]);
      }
    }
  }

  // Removes this state from the set, if count decrs to 0
  public void decr(int num) {

    if (useTreeMap) {
      final int count = map.get(num);
      if (count == 1) {
        map.remove(num);
      } else {
        map.put(num, count-1);
      }
      // Fall back to simple arrays once we touch zero again
      if (map.size() == 0) {
        useTreeMap = false;
        upto = 0;
      }
      return;
    }

    for(int i=0;i<upto;i++) {
      if (values[i] == num) {
        counts[i]--;
        if (counts[i] == 0) {
          final int limit = upto-1;
          while(i < limit) {
            values[i] = values[i+1];
            counts[i] = counts[i+1];
            i++;
          }
          upto = limit;
        }
        return;
      }
    }
    assert false;
  }

  public void computeHash() {
    if (useTreeMap) {
      if (map.size() > values.length) {
        final int size = ArrayUtil.oversize(map.size(), RamUsageEstimator.NUM_BYTES_INT);
        values = new int[size];
        counts = new int[size];
      }
      hashCode = map.size();
      upto = 0;
      for(int state : map.keySet()) {
        hashCode = 683*hashCode + state;
        values[upto++] = state;
      }
    } else {
      hashCode = upto;
      for(int i=0;i<upto;i++) {
        hashCode = 683*hashCode + values[i];
      }
    }
  }

  public FrozenIntSet freeze(int state) {
    final int[] c = new int[upto];
    System.arraycopy(values, 0, c, 0, upto);
    return new FrozenIntSet(c, hashCode, state);
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(Object _other) {
    if (_other == null) {
      return false;
    }
    if (!(_other instanceof FrozenIntSet)) {
      return false;
    }
    FrozenIntSet other = (FrozenIntSet) _other;
    if (hashCode != other.hashCode) {
      return false;
    }
    if (other.values.length != upto) {
      return false;
    }
    for(int i=0;i<upto;i++) {
      if (other.values[i] != values[i]) {
        return false;
      }
    }

    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder().append('[');
    for(int i=0;i<upto;i++) {
      if (i > 0) {
        sb.append(' ');
      }
      sb.append(values[i]).append(':').append(counts[i]);
    }
    sb.append(']');
    return sb.toString();
  }
  
  public final static class FrozenIntSet {
    final int[] values;
    final int hashCode;
    final int state;

    public FrozenIntSet(int[] values, int hashCode, int state) {
      this.values = values;
      this.hashCode = hashCode;
      this.state = state;
    }

    public FrozenIntSet(int num, int state) {
      this.values = new int[] {num};
      this.state = state;
      this.hashCode = 683+num;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public boolean equals(Object _other) {
      if (_other == null) {
        return false;
      }
      if (_other instanceof FrozenIntSet) {
        FrozenIntSet other = (FrozenIntSet) _other;
        if (hashCode != other.hashCode) {
          return false;
        }
        if (other.values.length != values.length) {
          return false;
        }
        for(int i=0;i<values.length;i++) {
          if (other.values[i] != values[i]) {
            return false;
          }
        }
        return true;
      } else if (_other instanceof SortedIntSet) {
        SortedIntSet other = (SortedIntSet) _other;
        if (hashCode != other.hashCode) {
          return false;
        }
        if (other.values.length != values.length) {
          return false;
        }
        for(int i=0;i<values.length;i++) {
          if (other.values[i] != values[i]) {
            return false;
          }
        }
        return true;
      }

      return false;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder().append('[');
      for(int i=0;i<values.length;i++) {
        if (i > 0) {
          sb.append(' ');
        }
        sb.append(values[i]);
      }
      sb.append(']');
      return sb.toString();
    }
  }
}
  
