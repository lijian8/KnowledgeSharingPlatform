package org.apache.lucene.codecs.lucene50;

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
import java.util.Collection;

import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.codecs.CompoundFormat;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

/**
 * Lucene 5.0 compound file format
 * <p>
 * Files:
 * <ul>
 *    <li><tt>.cfs</tt>: An optional "virtual" file consisting of all the other 
 *    index files for systems that frequently run out of file handles.
 *    <li><tt>.cfe</tt>: The "virtual" compound file's entry table holding all 
 *    entries in the corresponding .cfs file.
 * </ul>
 * <p>Description:</p>
 * <ul>
 *   <li>Compound (.cfs) --&gt; Header, FileData <sup>FileCount</sup>, Footer</li>
 *   <li>Compound Entry Table (.cfe) --&gt; Header, FileCount, &lt;FileName,
 *       DataOffset, DataLength&gt; <sup>FileCount</sup></li>
 *   <li>Header --&gt; {@link CodecUtil#writeIndexHeader IndexHeader}</li>
 *   <li>FileCount --&gt; {@link DataOutput#writeVInt VInt}</li>
 *   <li>DataOffset,DataLength,Checksum --&gt; {@link DataOutput#writeLong UInt64}</li>
 *   <li>FileName --&gt; {@link DataOutput#writeString String}</li>
 *   <li>FileData --&gt; raw file data</li>
 *   <li>Footer --&gt; {@link CodecUtil#writeFooter CodecFooter}</li>
 * </ul>
 * <p>Notes:</p>
 * <ul>
 *   <li>FileCount indicates how many files are contained in this compound file. 
 *       The entry table that follows has that many entries. 
 *   <li>Each directory entry contains a long pointer to the start of this file's data
 *       section, the files length, and a String with that file's name.
 * </ul>
 */
public final class Lucene50CompoundFormat extends CompoundFormat {

  /** Sole constructor. */
  public Lucene50CompoundFormat() {
  }
  
  @Override
  public Directory getCompoundReader(Directory dir, SegmentInfo si, IOContext context) throws IOException {
    return new Lucene50CompoundReader(dir, si, context);
  }

  @Override
  public void write(Directory dir, SegmentInfo si, Collection<String> files, IOContext context) throws IOException {
    String dataFile = IndexFileNames.segmentFileName(si.name, "", DATA_EXTENSION);
    String entriesFile = IndexFileNames.segmentFileName(si.name, "", ENTRIES_EXTENSION);
    
    try (IndexOutput data =    dir.createOutput(dataFile, context);
         IndexOutput entries = dir.createOutput(entriesFile, context)) {
      CodecUtil.writeIndexHeader(data,    DATA_CODEC, VERSION_CURRENT, si.getId(), "");
      CodecUtil.writeIndexHeader(entries, ENTRY_CODEC, VERSION_CURRENT, si.getId(), "");
      
      // write number of files
      entries.writeVInt(files.size());
      for (String file : files) {
        
        // write bytes for file
        long startOffset = data.getFilePointer();
        try (IndexInput in = dir.openInput(file, IOContext.READONCE)) {
          data.copyBytes(in, in.length());
        }
        long endOffset = data.getFilePointer();
        
        long length = endOffset - startOffset;
        
        // write entry for file
        entries.writeString(IndexFileNames.stripSegmentName(file));
        entries.writeLong(startOffset);
        entries.writeLong(length);
      }
      
      CodecUtil.writeFooter(data);
      CodecUtil.writeFooter(entries);
    }
  }

  @Override
  public String[] files(SegmentInfo si) {
    return new String[] {
      IndexFileNames.segmentFileName(si.name, "", DATA_EXTENSION),
      IndexFileNames.segmentFileName(si.name, "", ENTRIES_EXTENSION)
    };
  }

  /** Extension of compound file */
  static final String DATA_EXTENSION = "cfs";
  /** Extension of compound file entries */
  static final String ENTRIES_EXTENSION = "cfe";
  static final String DATA_CODEC = "Lucene50CompoundData";
  static final String ENTRY_CODEC = "Lucene50CompoundEntries";
  static final int VERSION_START = 0;
  static final int VERSION_CURRENT = VERSION_START;
}
