package org.apache.lucene.util;

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

import org.apache.lucene.store.DataOutput;

/**
 * A {@link DataOutput} that can be used to build a byte[].
 * @lucene.internal
 */
public final class GrowableByteArrayDataOutput extends DataOutput {

  /** The bytes */
  public byte[] bytes;
  /** The length */
  public int length;

  /** Create a {@link GrowableByteArrayDataOutput} with the given initial capacity. */
  public GrowableByteArrayDataOutput(int cp) {
    this.bytes = new byte[ArrayUtil.oversize(cp, 1)];
    this.length = 0;
  }

  @Override
  public void writeByte(byte b) {
    if (length >= bytes.length) {
      bytes = ArrayUtil.grow(bytes);
    }
    bytes[length++] = b;
  }

  @Override
  public void writeBytes(byte[] b, int off, int len) {
    final int newLength = length + len;
    bytes = ArrayUtil.grow(bytes, newLength);
    System.arraycopy(b, off, bytes, length, len);
    length = newLength;
  }

}
