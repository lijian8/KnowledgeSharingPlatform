package org.apache.lucene.search.payloads;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;

/**
 * Experimental class to get set of payloads for most standard Lucene queries.
 * Operates like Highlighter - IndexReader should only contain doc of interest,
 * best to use MemoryIndex.
 *
 * @lucene.experimental
 * 
 */
public class PayloadSpanUtil {
  private IndexReaderContext context;

  /**
   * @param context
   *          that contains doc with payloads to extract
   *          
   * @see IndexReader#getContext()
   */
  public PayloadSpanUtil(IndexReaderContext context) {
    this.context = context;
  }

  /**
   * Query should be rewritten for wild/fuzzy support.
   * 
   * @param query rewritten query
   * @return payloads Collection
   * @throws IOException if there is a low-level I/O error
   */
  public Collection<byte[]> getPayloadsForQuery(Query query) throws IOException {
    Collection<byte[]> payloads = new ArrayList<>();
    queryToSpanQuery(query, payloads);
    return payloads;
  }

  private void queryToSpanQuery(Query query, Collection<byte[]> payloads)
      throws IOException {
    if (query instanceof BooleanQuery) {
      BooleanClause[] queryClauses = ((BooleanQuery) query).getClauses();

      for (int i = 0; i < queryClauses.length; i++) {
        if (!queryClauses[i].isProhibited()) {
          queryToSpanQuery(queryClauses[i].getQuery(), payloads);
        }
      }

    } else if (query instanceof PhraseQuery) {
      Term[] phraseQueryTerms = ((PhraseQuery) query).getTerms();
      SpanQuery[] clauses = new SpanQuery[phraseQueryTerms.length];
      for (int i = 0; i < phraseQueryTerms.length; i++) {
        clauses[i] = new SpanTermQuery(phraseQueryTerms[i]);
      }

      int slop = ((PhraseQuery) query).getSlop();
      boolean inorder = false;

      if (slop == 0) {
        inorder = true;
      }

      SpanNearQuery sp = new SpanNearQuery(clauses, slop, inorder);
      sp.setBoost(query.getBoost());
      getPayloads(payloads, sp);
    } else if (query instanceof TermQuery) {
      SpanTermQuery stq = new SpanTermQuery(((TermQuery) query).getTerm());
      stq.setBoost(query.getBoost());
      getPayloads(payloads, stq);
    } else if (query instanceof SpanQuery) {
      getPayloads(payloads, (SpanQuery) query);
    } else if (query instanceof FilteredQuery) {
      queryToSpanQuery(((FilteredQuery) query).getQuery(), payloads);
    } else if (query instanceof DisjunctionMaxQuery) {

      for (Iterator<Query> iterator = ((DisjunctionMaxQuery) query).iterator(); iterator
          .hasNext();) {
        queryToSpanQuery(iterator.next(), payloads);
      }

    } else if (query instanceof MultiPhraseQuery) {
      final MultiPhraseQuery mpq = (MultiPhraseQuery) query;
      final List<Term[]> termArrays = mpq.getTermArrays();
      final int[] positions = mpq.getPositions();
      if (positions.length > 0) {

        int maxPosition = positions[positions.length - 1];
        for (int i = 0; i < positions.length - 1; ++i) {
          if (positions[i] > maxPosition) {
            maxPosition = positions[i];
          }
        }

        @SuppressWarnings({"rawtypes","unchecked"}) final List<Query>[] disjunctLists =
            new List[maxPosition + 1];
        int distinctPositions = 0;

        for (int i = 0; i < termArrays.size(); ++i) {
          final Term[] termArray = termArrays.get(i);
          List<Query> disjuncts = disjunctLists[positions[i]];
          if (disjuncts == null) {
            disjuncts = (disjunctLists[positions[i]] = new ArrayList<>(
                termArray.length));
            ++distinctPositions;
          }
          for (final Term term : termArray) {
            disjuncts.add(new SpanTermQuery(term));
          }
        }

        int positionGaps = 0;
        int position = 0;
        final SpanQuery[] clauses = new SpanQuery[distinctPositions];
        for (int i = 0; i < disjunctLists.length; ++i) {
          List<Query> disjuncts = disjunctLists[i];
          if (disjuncts != null) {
            clauses[position++] = new SpanOrQuery(disjuncts
                .toArray(new SpanQuery[disjuncts.size()]));
          } else {
            ++positionGaps;
          }
        }

        final int slop = mpq.getSlop();
        final boolean inorder = (slop == 0);

        SpanNearQuery sp = new SpanNearQuery(clauses, slop + positionGaps,
            inorder);
        sp.setBoost(query.getBoost());
        getPayloads(payloads, sp);
      }
    }
  }

  private void getPayloads(Collection<byte []> payloads, SpanQuery query)
      throws IOException {
    Map<Term,TermContext> termContexts = new HashMap<>();
    TreeSet<Term> terms = new TreeSet<>();
    query.extractTerms(terms);
    for (Term term : terms) {
      termContexts.put(term, TermContext.build(context, term));
    }
    for (LeafReaderContext leafReaderContext : context.leaves()) {
      final Spans spans = query.getSpans(leafReaderContext, leafReaderContext.reader().getLiveDocs(), termContexts);
      while (spans.next() == true) {
        if (spans.isPayloadAvailable()) {
          Collection<byte[]> payload = spans.getPayload();
          for (byte [] bytes : payload) {
            payloads.add(bytes);
          }
        }
      }
    }
  }
}
