package org.apache.lucene.analysis;

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

import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.AttributeSource;

import java.io.Reader;
import java.io.IOException;

/** A Tokenizer is a TokenStream whose input is a Reader.
  <p>
  This is an abstract class; subclasses must override {@link #incrementToken()}
  <p>
  NOTE: Subclasses overriding {@link #incrementToken()} must
  call {@link AttributeSource#clearAttributes()} before
  setting attributes.
 */
public abstract class Tokenizer extends TokenStream {  
  /** The text source for this Tokenizer. */
  protected Reader input = ILLEGAL_STATE_READER;
  
  /** Pending reader: not actually assigned to input until reset() */
  private Reader inputPending = ILLEGAL_STATE_READER;

  /**
   * Construct a tokenizer with no input, awaiting a call to {@link #setReader(java.io.Reader)}
   * to provide input.
   */
  protected Tokenizer() {
    //
  }

  /**
   * Construct a tokenizer with no input, awaiting a call to {@link #setReader(java.io.Reader)} to
   * provide input.
   * @param factory attribute factory.
   */
  protected Tokenizer(AttributeFactory factory) {
    super(factory);
  }

  /**
   * {@inheritDoc}
   * <p>
   * <b>NOTE:</b> 
   * The default implementation closes the input Reader, so
   * be sure to call <code>super.close()</code> when overriding this method.
   */
  @Override
  public void close() throws IOException {
    input.close();
    // LUCENE-2387: don't hold onto Reader after close, so
    // GC can reclaim
    inputPending = input = ILLEGAL_STATE_READER;
  }
  
  /** Return the corrected offset. If {@link #input} is a {@link CharFilter} subclass
   * this method calls {@link CharFilter#correctOffset}, else returns <code>currentOff</code>.
   * @param currentOff offset as seen in the output
   * @return corrected offset based on the input
   * @see CharFilter#correctOffset
   */
  protected final int correctOffset(int currentOff) {
    return (input instanceof CharFilter) ? ((CharFilter) input).correctOffset(currentOff) : currentOff;
  }

  /** Expert: Set a new reader on the Tokenizer.  Typically, an
   *  analyzer (in its tokenStream method) will use
   *  this to re-use a previously created tokenizer. */
  public final void setReader(Reader input) throws IOException {
    if (input == null) {
      throw new NullPointerException("input must not be null");
    } else if (this.input != ILLEGAL_STATE_READER) {
      throw new IllegalStateException("TokenStream contract violation: close() call missing");
    }
    this.inputPending = input;
    setReaderTestPoint();
  }
  
  @Override
  public void reset() throws IOException {
    super.reset();
    input = inputPending;
    inputPending = ILLEGAL_STATE_READER;
  }

  // only used for testing
  void setReaderTestPoint() {}
  
  private static final Reader ILLEGAL_STATE_READER = new Reader() {
    @Override
    public int read(char[] cbuf, int off, int len) {
      throw new IllegalStateException("TokenStream contract violation: reset()/close() call missing, " +
          "reset() called multiple times, or subclass does not call super.reset(). " +
          "Please see Javadocs of TokenStream class for more information about the correct consuming workflow.");
    }

    @Override
    public void close() {} 
  };
}

