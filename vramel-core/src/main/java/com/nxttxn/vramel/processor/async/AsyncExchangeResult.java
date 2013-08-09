/*
 * Copyright 2011-2012 the original author or authors.
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

package com.nxttxn.vramel.processor.async;

import com.google.common.base.Optional;
import com.nxttxn.vramel.Exchange;

/**
 * Represents a result that is returned asynchronously from an operation.<p>
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class AsyncExchangeResult {

    /**
     * The result of the operation. This will be null if the operation failed.
     */
    public final Optional<Exchange> result;

    /**

    /**
     * Did it succeeed?
     */
    public boolean succeeded() {
        return getException() == null;
    }

    /**
     * Did it fail?
     */
    public boolean failed() {
        return getException() != null;
    }

    /**
     * Create a successful AsyncResult
     * @param result The result
     */
    public AsyncExchangeResult(Exchange result) {
        this.result = Optional.of(result);

    }


    public Exception getException() {
        return result.get().getException();
    }
}
