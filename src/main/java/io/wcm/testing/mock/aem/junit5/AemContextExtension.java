/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2018 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.testing.mock.aem.junit5;

import static java.util.Collections.emptyList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit 5 extension that allows to inject {@link AemContext} (or subclasses of it) parameters in test methods,
 * and ensures that the context is set up and teared down properly for each test method.
 */
public class AemContextExtension implements ParameterResolver, AfterEachCallback {

  private static final Namespace AEM_CONTEXT_NAMESPACE = Namespace.create(AemContextExtension.class);
  private static final Class<ResourceResolverMockAemContext> DEFAULT_AEM_CONTEXT_TYPE = ResourceResolverMockAemContext.class;

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return AemContext.class.isAssignableFrom(parameterContext.getParameter().getType());
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    AemContext aemContext = getAemContext(extensionContext);
    if (aemContext == null) {
      aemContext = createAndStoreAemContext(parameterContext, extensionContext);
    }
    else if (paramIsNotInstanceOfExistingContext(parameterContext, aemContext)) {
      throw new ParameterResolutionException(
          "Found AemContext instance of type: " + aemContext.getClass().getName() + "\n"
              + "Required is: " + parameterContext.getParameter().getType().getName() + "\n"
              + "Verify that all test lifecycle methods (@BeforeEach, @Test, @AfterEach) "
              + "use the same AemContext type.");
    }
    return aemContext;
  }

  private AemContext getAemContext(ExtensionContext extensionContext) {
    return getStore(extensionContext).get(extensionContext.getRequiredTestMethod(), AemContext.class);
  }

  private Store getStore(ExtensionContext context) {
    return context.getStore(AEM_CONTEXT_NAMESPACE);
  }

  private AemContext createAndStoreAemContext(ParameterContext parameterContext, ExtensionContext extensionContext) {
    Type aemContextType = getAemContextType(parameterContext, extensionContext);
    if (aemContextType == AemContext.class) {
      aemContextType = DEFAULT_AEM_CONTEXT_TYPE;
    }
    try {
      Constructor constructor = ((Class<?>)aemContextType).getConstructor();
      AemContext aemContext = (AemContext)constructor.newInstance();
      aemContext.setUpContext();
      storeAemContext(extensionContext, aemContext);
      return aemContext;
    }
    // CHECKSTYLE:OFF
    catch (Exception ex) {
      // CHECKSTYLE:ON
      throw new IllegalStateException("Could not create " + aemContextType.getTypeName() + " instance.", ex);
    }
  }

  private Type getAemContextType(ParameterContext parameterContext, ExtensionContext extensionContext) {
    if (isAbstractAemContext(parameterContext)) {
      return getAemContextTypeFromTestMethod(extensionContext);
    }
    else {
      return parameterContext.getParameter().getType();
    }
  }

  private boolean isAbstractAemContext(ParameterContext parameterContext) {
    return parameterContext.getParameter().getType().equals(AemContext.class);
  }

  private void storeAemContext(ExtensionContext extensionContext, AemContext aemContext) {
    getStore(extensionContext).put(extensionContext.getRequiredTestMethod(), aemContext);
  }

  private void removeAemContext(ExtensionContext extensionContext, AemContext aemContext) {
    aemContext.tearDownContext();
    getStore(extensionContext).remove(extensionContext.getRequiredTestMethod());
  }

  private boolean paramIsNotInstanceOfExistingContext(ParameterContext parameterContext, AemContext aemContext) {
    return !parameterContext.getParameter().getType().isInstance(aemContext);
  }

  private Class<?> getAemContextTypeFromTestMethod(ExtensionContext extensionContext) {
    return extensionContext.getTestMethod()
        .map(Method::getParameterTypes)
        .map(Arrays::asList)
        .orElse(emptyList())
        .stream()
        .filter(type -> type.isInstance(AemContext.class))
        .findFirst()
        .orElse(DEFAULT_AEM_CONTEXT_TYPE);
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    AemContext aemContext = getAemContext(extensionContext);
    if (aemContext != null) {
      removeAemContext(extensionContext, aemContext);
    }
  }

}
