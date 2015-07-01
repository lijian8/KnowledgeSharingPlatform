package org.apache.lucene.queryparser.flexible.core;

import org.apache.lucene.queryparser.flexible.core.builders.QueryBuilder;
import org.apache.lucene.queryparser.flexible.core.config.QueryConfigHandler;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.parser.SyntaxParser;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessor;

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

  
/**
 * <p>
 * This class is a helper for the query parser framework, it does all the three
 * query parser phrases at once: text parsing, query processing and query
 * building.
 * </p>
 * <p>
 * It contains methods that allows the user to change the implementation used on
 * the three phases.
 * </p>
 * 
 * @see QueryNodeProcessor
 * @see SyntaxParser
 * @see QueryBuilder
 * @see QueryConfigHandler
 */
public class QueryParserHelper {

  private QueryNodeProcessor processor;

  private SyntaxParser syntaxParser;

  private QueryBuilder builder;

  private QueryConfigHandler config;

  /**
   * Creates a query parser helper object using the specified configuration,
   * text parser, processor and builder.
   * 
   * @param queryConfigHandler
   *          the query configuration handler that will be initially set to this
   *          helper
   * @param syntaxParser
   *          the text parser that will be initially set to this helper
   * @param processor
   *          the query processor that will be initially set to this helper
   * @param builder
   *          the query builder that will be initially set to this helper
   * 
   * @see QueryNodeProcessor
   * @see SyntaxParser
   * @see QueryBuilder
   * @see QueryConfigHandler
   */
  public QueryParserHelper(QueryConfigHandler queryConfigHandler, SyntaxParser syntaxParser, QueryNodeProcessor processor,
      QueryBuilder builder) {
    this.syntaxParser = syntaxParser;
    this.config = queryConfigHandler;
    this.processor = processor;
    this.builder = builder;

    if (processor != null) {
      processor.setQueryConfigHandler(queryConfigHandler);
    }

  }

  /**
   * Returns the processor object used to process the query node tree, it
   * returns <code>null</code> if no processor is used.
   * 
   * @return the actual processor used to process the query node tree,
   *         <code>null</code> if no processor is used
   * 
   * @see QueryNodeProcessor
   * @see #setQueryNodeProcessor(QueryNodeProcessor)
   */
  public QueryNodeProcessor getQueryNodeProcessor() {
    return processor;
  }

  /**
   * Sets the processor that will be used to process the query node tree. If
   * there is any {@link QueryConfigHandler} returned by
   * {@link #getQueryConfigHandler()}, it will be set on the processor. The
   * argument can be <code>null</code>, which means that no processor will be
   * used to process the query node tree.
   * 
   * @param processor
   *          the processor that will be used to process the query node tree,
   *          this argument can be <code>null</code>
   * 
   * @see #getQueryNodeProcessor()
   * @see QueryNodeProcessor
   */
  public void setQueryNodeProcessor(QueryNodeProcessor processor) {
    this.processor = processor;
    this.processor.setQueryConfigHandler(getQueryConfigHandler());

  }

  /**
   * Sets the text parser that will be used to parse the query string, it cannot
   * be <code>null</code>.
   * 
   * @param syntaxParser
   *          the text parser that will be used to parse the query string
   * 
   * @see #getSyntaxParser()
   * @see SyntaxParser
   */
  public void setSyntaxParser(SyntaxParser syntaxParser) {

    if (syntaxParser == null) {
      throw new IllegalArgumentException("textParser should not be null!");
    }

    this.syntaxParser = syntaxParser;

  }

  /**
   * The query builder that will be used to build an object from the query node
   * tree. It cannot be <code>null</code>.
   * 
   * @param queryBuilder
   *          the query builder used to build something from the query node tree
   * 
   * @see #getQueryBuilder()
   * @see QueryBuilder
   */
  public void setQueryBuilder(QueryBuilder queryBuilder) {

    if (queryBuilder == null) {
      throw new IllegalArgumentException("queryBuilder should not be null!");
    }

    this.builder = queryBuilder;

  }

  /**
   * Returns the query configuration handler, which is used during the query
   * node tree processing. It can be <code>null</code>.
   * 
   * @return the query configuration handler used on the query processing,
   *         <code>null</code> if not query configuration handler is defined
   * 
   * @see QueryConfigHandler
   * @see #setQueryConfigHandler(QueryConfigHandler)
   */
  public QueryConfigHandler getQueryConfigHandler() {
    return config;
  }

  /**
   * Returns the query builder used to build a object from the query node tree.
   * The object produced by this builder is returned by
   * {@link #parse(String, String)}.
   * 
   * @return the query builder
   * 
   * @see #setQueryBuilder(QueryBuilder)
   * @see QueryBuilder
   */
  public QueryBuilder getQueryBuilder() {
    return this.builder;
  }

  /**
   * Returns the text parser used to build a query node tree from a query
   * string. The default text parser instance returned by this method is a
   * {@link SyntaxParser}.
   * 
   * @return the text parse used to build query node trees.
   * 
   * @see SyntaxParser
   * @see #setSyntaxParser(SyntaxParser)
   */
  public SyntaxParser getSyntaxParser() {
    return this.syntaxParser;
  }

  /**
   * Sets the query configuration handler that will be used during query
   * processing. It can be <code>null</code>. It's also set to the processor
   * returned by {@link #getQueryNodeProcessor()}.
   * 
   * @param config
   *          the query configuration handler used during query processing, it
   *          can be <code>null</code>
   * 
   * @see #getQueryConfigHandler()
   * @see QueryConfigHandler
   */
  public void setQueryConfigHandler(QueryConfigHandler config) {
    this.config = config;
    QueryNodeProcessor processor = getQueryNodeProcessor();

    if (processor != null) {
      processor.setQueryConfigHandler(config);
    }

  }

  /**
   * Parses a query string to an object, usually some query object. <br/>
   * <br/>
   * In this method the three phases are executed: <br/>
   * <br/>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1st - the query string is parsed using the
   * text parser returned by {@link #getSyntaxParser()}, the result is a query
   * node tree <br/>
   * <br/>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2nd - the query node tree is processed by the
   * processor returned by {@link #getQueryNodeProcessor()} <br/>
   * <br/>
   * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;3th - a object is built from the query node
   * tree using the builder returned by {@link #getQueryBuilder()}
   * 
   * @param query
   *          the query string
   * @param defaultField
   *          the default field used by the text parser
   * 
   * @return the object built from the query
   * 
   * @throws QueryNodeException
   *           if something wrong happens along the three phases
   */
  public Object parse(String query, String defaultField)
      throws QueryNodeException {
    QueryNode queryTree = getSyntaxParser().parse(query, defaultField);

    QueryNodeProcessor processor = getQueryNodeProcessor();

    if (processor != null) {
      queryTree = processor.process(queryTree);
    }

    return getQueryBuilder().build(queryTree);

  }

}
