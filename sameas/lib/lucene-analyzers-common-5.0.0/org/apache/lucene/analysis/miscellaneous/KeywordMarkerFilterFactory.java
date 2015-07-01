package org.apache.lucene.analysis.miscellaneous;

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
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 * Factory for {@link KeywordMarkerFilter}.
 * <pre class="prettyprint">
 * &lt;fieldType name="text_keyword" class="solr.TextField" positionIncrementGap="100"&gt;
 *   &lt;analyzer&gt;
 *     &lt;tokenizer class="solr.WhitespaceTokenizerFactory"/&gt;
 *     &lt;filter class="solr.KeywordMarkerFilterFactory" protected="protectedkeyword.txt" pattern="^.+er$" ignoreCase="false"/&gt;
 *   &lt;/analyzer&gt;
 * &lt;/fieldType&gt;</pre>
 */
public class KeywordMarkerFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {
  public static final String PROTECTED_TOKENS = "protected";
  public static final String PATTERN = "pattern";
  private final String wordFiles;
  private final String stringPattern;
  private final boolean ignoreCase;
  private Pattern pattern;
  private CharArraySet protectedWords;
  
  /** Creates a new KeywordMarkerFilterFactory */
  public KeywordMarkerFilterFactory(Map<String,String> args) {
    super(args);
    wordFiles = get(args, PROTECTED_TOKENS);
    stringPattern = get(args, PATTERN);
    ignoreCase = getBoolean(args, "ignoreCase", false);
    if (!args.isEmpty()) {
      throw new IllegalArgumentException("Unknown parameters: " + args);
    }
  }
  
  @Override
  public void inform(ResourceLoader loader) throws IOException {
    if (wordFiles != null) {  
      protectedWords = getWordSet(loader, wordFiles, ignoreCase);
    }
    if (stringPattern != null) {
      pattern = ignoreCase ? Pattern.compile(stringPattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE) : Pattern.compile(stringPattern);
    }
  }
  
  public boolean isIgnoreCase() {
    return ignoreCase;
  }

  @Override
  public TokenStream create(TokenStream input) {
    if (pattern != null) {
      input = new PatternKeywordMarkerFilter(input, pattern);
    }
    if (protectedWords != null) {
      input = new SetKeywordMarkerFilter(input, protectedWords);
    }
    return input;
  }
}
