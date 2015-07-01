package org.apache.lucene.queryparser.flexible.standard.nodes;

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

import java.util.ArrayList;

import org.apache.lucene.queryparser.flexible.core.nodes.FieldValuePairQueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.FieldableNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNodeImpl;
import org.apache.lucene.queryparser.flexible.core.nodes.RangeQueryNode;
import org.apache.lucene.queryparser.flexible.core.parser.EscapeQuerySyntax;
import org.apache.lucene.queryparser.flexible.core.util.StringUtils;

/**
 * This class should be extended by nodes intending to represent range queries.
 * 
 * @param <T>
 *          the type of the range query bounds (lower and upper)
 */
public class AbstractRangeQueryNode<T extends FieldValuePairQueryNode<?>>
    extends QueryNodeImpl implements RangeQueryNode<FieldValuePairQueryNode<?>> {
  
  private boolean lowerInclusive, upperInclusive;
  
  /**
   * Constructs an {@link AbstractRangeQueryNode}, it should be invoked only by
   * its extenders.
   */
  protected AbstractRangeQueryNode() {
    setLeaf(false);
    allocate();
  }
  
  /**
   * Returns the field associated with this node.
   * 
   * @return the field associated with this node
   * 
   * @see FieldableNode
   */
  @Override
  public CharSequence getField() {
    CharSequence field = null;
    T lower = getLowerBound();
    T upper = getUpperBound();
    
    if (lower != null) {
      field = lower.getField();
      
    } else if (upper != null) {
      field = upper.getField();
    }
    
    return field;
    
  }
  
  /**
   * Sets the field associated with this node.
   * 
   * @param fieldName the field associated with this node
   */
  @Override
  public void setField(CharSequence fieldName) {
    T lower = getLowerBound();
    T upper = getUpperBound();
    
    if (lower != null) {
      lower.setField(fieldName);
    }
    
    if (upper != null) {
      upper.setField(fieldName);
    }
    
  }
  
  /**
   * Returns the lower bound node.
   * 
   * @return the lower bound node.
   */
  @Override
  @SuppressWarnings("unchecked")
  public T getLowerBound() {
    return (T) getChildren().get(0);
  }
  
  /**
   * Returns the upper bound node.
   * 
   * @return the upper bound node.
   */
  @Override
  @SuppressWarnings("unchecked")
  public T getUpperBound() {
    return (T) getChildren().get(1);
  }
  
  /**
   * Returns whether the lower bound is inclusive or exclusive.
   * 
   * @return <code>true</code> if the lower bound is inclusive, otherwise, <code>false</code>
   */
  @Override
  public boolean isLowerInclusive() {
    return lowerInclusive;
  }
  
  /**
   * Returns whether the upper bound is inclusive or exclusive.
   * 
   * @return <code>true</code> if the upper bound is inclusive, otherwise, <code>false</code>
   */
  @Override
  public boolean isUpperInclusive() {
    return upperInclusive;
  }
  
  /**
   * Sets the lower and upper bounds.
   * 
   * @param lower the lower bound, <code>null</code> if lower bound is open
   * @param upper the upper bound, <code>null</code> if upper bound is open
   * @param lowerInclusive <code>true</code> if the lower bound is inclusive, otherwise, <code>false</code>
   * @param upperInclusive <code>true</code> if the upper bound is inclusive, otherwise, <code>false</code>
   * 
   * @see #getLowerBound()
   * @see #getUpperBound()
   * @see #isLowerInclusive()
   * @see #isUpperInclusive()
   */
  public void setBounds(T lower, T upper, boolean lowerInclusive,
      boolean upperInclusive) {
    
    if (lower != null && upper != null) {
      String lowerField = StringUtils.toString(lower.getField());
      String upperField = StringUtils.toString(upper.getField());
      
      if ((upperField != null || lowerField != null)
          && ((upperField != null && !upperField.equals(lowerField)) || !lowerField
              .equals(upperField))) {
        throw new IllegalArgumentException(
            "lower and upper bounds should have the same field name!");
      }
      
      this.lowerInclusive = lowerInclusive;
      this.upperInclusive = upperInclusive;
      
      ArrayList<QueryNode> children = new ArrayList<>(2);
      children.add(lower);
      children.add(upper);
      
      set(children);
      
    }
    
  }
  
  @Override
  public CharSequence toQueryString(EscapeQuerySyntax escapeSyntaxParser) {
    StringBuilder sb = new StringBuilder();
    
    T lower = getLowerBound();
    T upper = getUpperBound();
    
    if (lowerInclusive) {
      sb.append('[');
      
    } else {
      sb.append('{');
    }
    
    if (lower != null) {
      sb.append(lower.toQueryString(escapeSyntaxParser));
      
    } else {
      sb.append("...");
    }
    
    sb.append(' ');
    
    if (upper != null) {
      sb.append(upper.toQueryString(escapeSyntaxParser));
      
    } else {
      sb.append("...");
    }
    
    if (upperInclusive) {
      sb.append(']');
      
    } else {
      sb.append('}');
    }
    
    return sb.toString();
    
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("<").append(getClass().getCanonicalName());
    sb.append(" lowerInclusive=").append(isLowerInclusive());
    sb.append(" upperInclusive=").append(isUpperInclusive());
    sb.append(">\n\t");
    sb.append(getUpperBound()).append("\n\t");
    sb.append(getLowerBound()).append("\n");
    sb.append("</").append(getClass().getCanonicalName()).append(">\n");

    return sb.toString();

  }
  
}
