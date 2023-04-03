/*
 * Copyright (c) 2005-2022 Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xceptance.xlt.engine;

import org.htmlunit.WebClient;
import org.htmlunit.javascript.background.BackgroundJavaScriptFactory;
import org.htmlunit.javascript.background.JavaScriptExecutor;

/**
 * Specialization of {@link BackgroundJavaScriptFactory} that uses our implementation of {@link JavaScriptExecutor}.
 * 
 * @author Hartmut Arlt (Xceptance Software Technologies GmbH)
 */
public class XltBackgroundJavaScriptFactory extends BackgroundJavaScriptFactory
{
    /**
     * {@inheritDoc}
     */
    @Override
    public JavaScriptExecutor createJavaScriptExecutor(final WebClient webClient)
    {
        return new XltJavaScriptExecutor(webClient);
    };
}
