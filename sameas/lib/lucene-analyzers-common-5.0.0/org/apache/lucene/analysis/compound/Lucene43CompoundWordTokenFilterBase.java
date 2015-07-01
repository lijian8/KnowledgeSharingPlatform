package org.apache.lucene.analysis.compound;

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
import java.util.LinkedList;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.AttributeSource;

/**
 * Base class for decomposition token filters using pre-4.4 behavior.
 * <p>
 * @deprecated Use {@link CompoundWordTokenFilterBase}
 */
@Deprecated
public abstract class Lucene43CompoundWordTokenFilterBase extends TokenFilter {
  /**
   * The default for minimal word length that gets decomposed
   */
  public static final int DEFAULT_MIN_WORD_SIZE = 5;

  /**
   * The default for minimal length of subwords that get propagated to the output of this filter
   */
  public static final int DEFAULT_MIN_SUBWORD_SIZE = 2;

  /**
   * The default for maximal length of subwords that get propagated to the output of this filter
   */
  public static final int DEFAULT_MAX_SUBWORD_SIZE = 15;

  protected final CharArraySet dictionary;
  protected final LinkedList<CompoundToken> tokens;
  protected final int minWordSize;
  protected final int minSubwordSize;
  protected final int maxSubwordSize;
  protected final boolean onlyLongestMatch;
  
  protected final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
  protected final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
  private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
  
  private AttributeSource.State current;

  protected Lucene43CompoundWordTokenFilterBase(TokenStream input, CharArraySet dictionary, boolean onlyLongestMatch) {
    this(input,dictionary,DEFAULT_MIN_WORD_SIZE,DEFAULT_MIN_SUBWORD_SIZE,DEFAULT_MAX_SUBWORD_SIZE, onlyLongestMatch);
  }

  protected Lucene43CompoundWordTokenFilterBase(TokenStream input, CharArraySet dictionary) {
    this(input,dictionary,DEFAULT_MIN_WORD_SIZE,DEFAULT_MIN_SUBWORD_SIZE,DEFAULT_MAX_SUBWORD_SIZE, false);
  }

  protected Lucene43CompoundWordTokenFilterBase(TokenStream input, CharArraySet dictionary, int minWordSize, int minSubwordSize, int maxSubwordSize, boolean onlyLongestMatch) {
    super(input);
    this.tokens=new LinkedList<>();
    if (minWordSize < 0) {
      throw new IllegalArgumentException("minWordSize cannot be negative");
    }
    this.minWordSize=minWordSize;
    if (minSubwordSize < 0) {
      throw new IllegalArgumentException("minSubwordSize cannot be negative");
    }
    this.minSubwordSize=minSubwordSize;
    if (maxSubwordSize < 0) {
      throw new IllegalArgumentException("maxSubwordSize cannot be negative");
    }
    this.maxSubwordSize=maxSubwordSize;
    this.onlyLongestMatch=onlyLongestMatch;
    this.dictionary = dictionary;
  }
  
  @Override
  public final boolean incrementToken() throws IOException {
    if (!tokens.isEmpty()) {
      assert current != null;
      CompoundToken token = tokens.removeFirst();
      restoreState(current); // keep all other attributes untouched
      termAtt.setEmpty().append(token.txt);
      offsetAtt.setOffset(token.startOffset, token.endOffset);
      posIncAtt.setPositionIncrement(0);
      return true;
    }

    current = null; // not really needed, but for safety
    if (input.incrementToken()) {
      // Only words longer than minWordSize get processed
      if (termAtt.length() >= this.minWordSize) {
        decompose();
        // only capture the state if we really need it for producing new tokens
        if (!tokens.isEmpty()) {
          current = captureState();
        }
      }
      // return original token:
      return true;
    } else {
      return false;
    }
  }

  /** Decomposes the current {@link #termAtt} and places {@link CompoundToken} instances in the {@link #tokens} list.
   * The original token may not be placed in the list, as it is automatically passed through this filter.
   */
  protected abstract void decompose();

  @Override
  public void reset() throws IOException {
    super.reset();
    tokens.clear();
    current = null;
  }
  
  /**
   * Helper class to hold decompounded token information
   */
  protected class CompoundToken {
    public final CharSequence txt;
    public final int startOffset, endOffset;

    /** Construct the compound token based on a slice of the current {@link Lucene43CompoundWordTokenFilterBase#termAtt}. */
    public CompoundToken(int offset, int length) {
      this.txt = Lucene43CompoundWordTokenFilterBase.this.termAtt.subSequence(offset, offset + length);
      
      // offsets of the original word
      int startOff = Lucene43CompoundWordTokenFilterBase.this.offsetAtt.startOffset();
      int endOff = Lucene43CompoundWordTokenFilterBase.this.offsetAtt.endOffset();
      
      if (endOff - startOff != Lucene43CompoundWordTokenFilterBase.this.termAtt.length()) {
        // if length by start + end offsets doesn't match the term text then assume
        // this is a synonym and don't adjust the offsets.
        this.startOffset = startOff;
        this.endOffset = endOff;
      } else {
        final int newStart = startOff + offset;
        this.startOffset = newStart;
        this.endOffset = newStart + length;
      }
    }

  }  
}
