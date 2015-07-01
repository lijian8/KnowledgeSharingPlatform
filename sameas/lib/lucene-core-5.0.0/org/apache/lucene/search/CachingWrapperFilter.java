package org.apache.lucene.search;

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

import static org.apache.lucene.search.DocIdSet.EMPTY;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Accountables;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.RoaringDocIdSet;

/**
 * Wraps another {@link Filter}'s result and caches it.  The purpose is to allow
 * filters to simply filter, and then wrap with this class
 * to add caching.
 */
public class CachingWrapperFilter extends Filter implements Accountable {
  private final Filter filter;
  private final FilterCachingPolicy policy;
  private final Map<Object,DocIdSet> cache = Collections.synchronizedMap(new WeakHashMap<Object,DocIdSet>());

  /** Wraps another filter's result and caches it according to the provided policy.
   * @param filter Filter to cache results of
   * @param policy policy defining which filters should be cached on which segments
   */
  public CachingWrapperFilter(Filter filter, FilterCachingPolicy policy) {
    this.filter = filter;
    this.policy = policy;
  }

  /** Same as {@link CachingWrapperFilter#CachingWrapperFilter(Filter, FilterCachingPolicy)}
   *  but enforces the use of the
   *  {@link FilterCachingPolicy.CacheOnLargeSegments#DEFAULT} policy. */
  public CachingWrapperFilter(Filter filter) {
    this(filter, FilterCachingPolicy.CacheOnLargeSegments.DEFAULT);
  }

  /**
   * Gets the contained filter.
   * @return the contained filter.
   */
  public Filter getFilter() {
    return filter;
  }

  /** 
   *  Provide the DocIdSet to be cached, using the DocIdSet provided
   *  by the wrapped Filter. <p>This implementation returns the given {@link DocIdSet},
   *  if {@link DocIdSet#isCacheable} returns <code>true</code>, else it calls
   *  {@link #cacheImpl(DocIdSetIterator, org.apache.lucene.index.LeafReader)}
   *  <p>Note: This method returns {@linkplain DocIdSet#EMPTY} if the given docIdSet
   *  is <code>null</code> or if {@link DocIdSet#iterator()} return <code>null</code>. The empty
   *  instance is use as a placeholder in the cache instead of the <code>null</code> value.
   */
  protected DocIdSet docIdSetToCache(DocIdSet docIdSet, LeafReader reader) throws IOException {
    if (docIdSet == null || docIdSet.isCacheable()) {
      return docIdSet;
    } else {
      final DocIdSetIterator it = docIdSet.iterator();
      if (it == null) {
        return null;
      } else {
        return cacheImpl(it, reader);
      }
    }
  }
  
  /**
   * Default cache implementation: uses {@link RoaringDocIdSet}.
   */
  protected DocIdSet cacheImpl(DocIdSetIterator iterator, LeafReader reader) throws IOException {
    return new RoaringDocIdSet.Builder(reader.maxDoc()).add(iterator).build();
  }

  // for testing
  int hitCount, missCount;

  @Override
  public DocIdSet getDocIdSet(LeafReaderContext context, final Bits acceptDocs) throws IOException {
    final LeafReader reader = context.reader();
    final Object key = reader.getCoreCacheKey();

    DocIdSet docIdSet = cache.get(key);
    if (docIdSet != null) {
      hitCount++;
    } else {
      docIdSet = filter.getDocIdSet(context, null);
      if (policy.shouldCache(filter, context, docIdSet)) {
        missCount++;
        docIdSet = docIdSetToCache(docIdSet, reader);
        if (docIdSet == null) {
          // We use EMPTY as a sentinel for the empty set, which is cacheable
          docIdSet = EMPTY;
        }
        assert docIdSet.isCacheable();
        cache.put(key, docIdSet);
      }
    }

    return docIdSet == EMPTY ? null : BitsFilteredDocIdSet.wrap(docIdSet, acceptDocs);
  }
  
  @Override
  public String toString() {
    return getClass().getSimpleName() + "("+filter+")";
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || !getClass().equals(o.getClass())) return false;
    final CachingWrapperFilter other = (CachingWrapperFilter) o;
    return this.filter.equals(other.filter);
  }

  @Override
  public int hashCode() {
    return (filter.hashCode() ^ getClass().hashCode());
  }

  @Override
  public long ramBytesUsed() {

    // Sync only to pull the current set of values:
    List<DocIdSet> docIdSets;
    synchronized(cache) {
      docIdSets = new ArrayList<>(cache.values());
    }

    long total = 0;
    for(DocIdSet dis : docIdSets) {
      total += dis.ramBytesUsed();
    }

    return total;
  }

  @Override
  public Collection<Accountable> getChildResources() {
    // Sync to pull the current set of values:
    synchronized (cache) {
      // no need to clone, Accountable#namedAccountables already copies the data
      return Accountables.namedAccountables("segment", cache);
    }
  }
}
