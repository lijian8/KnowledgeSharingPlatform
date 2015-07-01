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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * An AttributeFactory creates instances of {@link AttributeImpl}s.
 */
public abstract class AttributeFactory {
  
  /**
   * Returns an {@link AttributeImpl} for the supplied {@link Attribute} interface class.
   */
  public abstract AttributeImpl createAttributeInstance(Class<? extends Attribute> attClass);
  
  /**
   * Returns a correctly typed {@link MethodHandle} for the no-arg ctor of the given class.
   */
  static final MethodHandle findAttributeImplCtor(Class<? extends AttributeImpl> clazz) {
    try {
      return lookup.findConstructor(clazz, NO_ARG_CTOR).asType(NO_ARG_RETURNING_ATTRIBUTEIMPL);
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new IllegalArgumentException("Cannot lookup accessible no-arg constructor for: " + clazz.getName(), e);
    }
  }
  
  private static final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
  private static final MethodType NO_ARG_CTOR = MethodType.methodType(void.class);
  private static final MethodType NO_ARG_RETURNING_ATTRIBUTEIMPL = MethodType.methodType(AttributeImpl.class);
  
  /**
   * This is the default factory that creates {@link AttributeImpl}s using the
   * class name of the supplied {@link Attribute} interface class by appending <code>Impl</code> to it.
   */
  public static final AttributeFactory DEFAULT_ATTRIBUTE_FACTORY = new DefaultAttributeFactory();
  
  private static final class DefaultAttributeFactory extends AttributeFactory {
    private final ClassValue<MethodHandle> constructors = new ClassValue<MethodHandle>() {
      @Override
      protected MethodHandle computeValue(Class<?> attClass) {
        return findAttributeImplCtor(findImplClass(attClass.asSubclass(Attribute.class)));
      }
    };

    DefaultAttributeFactory() {}
  
    @Override
    public AttributeImpl createAttributeInstance(Class<? extends Attribute> attClass) {
      try {
        return (AttributeImpl) constructors.get(attClass).invokeExact();
      } catch (Throwable t) {
        rethrow(t);
        throw new AssertionError();
      }
    }
    
    private Class<? extends AttributeImpl> findImplClass(Class<? extends Attribute> attClass) {
      try {
        return Class.forName(attClass.getName() + "Impl", true, attClass.getClassLoader()).asSubclass(AttributeImpl.class);
      } catch (ClassNotFoundException cnfe) {
        throw new IllegalArgumentException("Cannot find implementing class for: " + attClass.getName());
      }      
    }
  }
  
  /** <b>Expert</b>: AttributeFactory returning an instance of the given {@code clazz} for the
   * attributes it implements. For all other attributes it calls the given delegate factory
   * as fallback. This class can be used to prefer a specific {@code AttributeImpl} which
   * combines multiple attributes over separate classes.
   * @lucene.internal
   */
  public abstract static class StaticImplementationAttributeFactory<A extends AttributeImpl> extends AttributeFactory {
    private final AttributeFactory delegate;
    private final Class<A> clazz;
    
    /** <b>Expert</b>: Creates an AttributeFactory returning {@code clazz} as instance for the
     * attributes it implements and for all other attributes calls the given delegate factory. */
    public StaticImplementationAttributeFactory(AttributeFactory delegate, Class<A> clazz) {
      this.delegate = delegate;
      this.clazz = clazz;
    }
    
    @Override
    public final AttributeImpl createAttributeInstance(Class<? extends Attribute> attClass) {
      return attClass.isAssignableFrom(clazz) ? createInstance() : delegate.createAttributeInstance(attClass);
    }
    
    /** Creates an instance of {@code A}. */
    protected abstract A createInstance();
    
    @Override
    public boolean equals(Object other) {
      if (this == other)
        return true;
      if (other == null || other.getClass() != this.getClass())
        return false;
      @SuppressWarnings("rawtypes")
      final StaticImplementationAttributeFactory af = (StaticImplementationAttributeFactory) other;
      return this.delegate.equals(af.delegate) && this.clazz == af.clazz;
    }
    
    @Override
    public int hashCode() {
      return 31 * delegate.hashCode() + clazz.hashCode();
    }
  }
  
  /** Returns an AttributeFactory returning an instance of the given {@code clazz} for the
   * attributes it implements. The given {@code clazz} must have a public no-arg constructor.
   * For all other attributes it calls the given delegate factory as fallback.
   * This method can be used to prefer a specific {@code AttributeImpl} which combines
   * multiple attributes over separate classes.
   * <p>Please save instances created by this method in a static final field, because
   * on each call, this does reflection for creating a {@link MethodHandle}.
   */
  public static <A extends AttributeImpl> AttributeFactory getStaticImplementation(AttributeFactory delegate, Class<A> clazz) {
    final MethodHandle constr = findAttributeImplCtor(clazz);
    return new StaticImplementationAttributeFactory<A>(delegate, clazz) {
      @Override
      protected A createInstance() {
        try {
          return (A) constr.invokeExact();
        } catch (Throwable t) {
          rethrow(t);
          throw new AssertionError();
        }
      }
    };
  }
  
  // Hack to rethrow unknown Exceptions from {@link MethodHandle#invoke}:
  // TODO: remove the impl in test-framework, this one is more elegant :-)
  static void rethrow(Throwable t) {
    AttributeFactory.<Error>rethrow0(t);
  }
  
  @SuppressWarnings("unchecked")
  private static <T extends Throwable> void rethrow0(Throwable t) throws T {
    throw (T) t;
  }
  
}