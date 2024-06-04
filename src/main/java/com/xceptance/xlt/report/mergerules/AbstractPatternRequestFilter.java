/*
 * Copyright (c) 2005-2024 Xceptance Software Technologies GmbH
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
package com.xceptance.xlt.report.mergerules;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.MatchResult;
import com.justinblank.strings.DFACompiler;
import com.justinblank.strings.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.xceptance.common.collection.LRUFastHashMap;
import com.xceptance.common.lang.ThrowableUtils;
import com.xceptance.common.util.RegExUtils;
import com.xceptance.xlt.api.engine.RequestData;

/**
 * Base class for all request filters that use regular expressions to identify matching requests.
 */
public abstract class AbstractPatternRequestFilter extends AbstractRequestFilter
{
    /**
     * Cache the expensive stuff, we are a per thread instance. Can be empty!
     */
    private final LRUFastHashMap<CharSequence, MatchResult> cache;

    /**
     * Just a place holder for a NULL
     */
    private static final MatchResult NULL = java.util.regex.Pattern.compile(".*").matcher(StringUtils.EMPTY).toMatchResult();
    
    private static final ConcurrentHashMap<String, Pattern> needlePatternCache = new ConcurrentHashMap<>(300);

    /**
     * The matcher we use when we don't want to cache anything
     */
    private final java.util.regex.Matcher matcher;
    private final com.justinblank.strings.Pattern needlePattern;

    /**
     * Whether or not this is an exclusion rule.
     */
    private final boolean isExclude;

    /**
     * Constructor.
     *
     * @param typeCode
     *            the type code of this request filter
     * @param regex
     *            the regular expression to identify matching requests
     */
    public AbstractPatternRequestFilter(final String typeCode, final String regex)
    {
        // default cache
        this(typeCode, regex, false, 100);
    }

    /**
     * Constructor.
     *
     * @param typeCode
     *            the type code of this request filter
     * @param regex
     *            the regular expression to identify matching requests
     * @param exclude
     *            whether or not this is an exclusion rule
     */
    public AbstractPatternRequestFilter(final String typeCode, final String regex, final boolean exclude, final int cacheSize)
    {
        super(typeCode);

        if(StringUtils.isBlank(regex))
        {
            this.matcher = null;
            this.needlePattern = null;
        }
        else
        {
            this.matcher = RegExUtils.getPattern(regex, 0).matcher("any");
            needlePattern = needlePatternCache.computeIfAbsent(regex, r -> DFACompiler.compile(regex, ""+regex.hashCode()));
        }

        this.isExclude = exclude;
        
        this.cache = cacheSize > 0 ? new LRUFastHashMap<>(cacheSize) : null;
    }

    /**
     * Returns the text to examine from the passed request data object.d
     *
     * @return the text
     */
    protected abstract CharSequence getText(final RequestData requestData);

    /**
     * {@inheritDoc}
     */
    @Override
    public Object appliesTo(final RequestData requestData)
    {
        if (matcher == null)
        {
            // empty is always fine, we just want to get the full text -> return a non-null dummy object
            return Boolean.TRUE;
        }

        // get the data to match against
        final CharSequence text = getText(requestData);

        // only cache if we want that, there are areas where caching does not make sense and wastes
        // a lot of time
        if (cache == null)
        {
            // Needle is used to check for a match in general. Java matcher is returned for further use.

            // when we return the matcher, it will be evaluated instantly and
            // hence is reuseable during the next call, this saves memory
            // because a matcher is large
            return (needlePattern.matcher(text.toString()).matches() ^ isExclude) ? this.matcher.reset(text) : null;
        }
        else
        {
            MatchResult result = this.cache.get(text);
            if (result == null)
            {
                // not found, produce and cache
                if (this.needlePattern.matcher(text.toString()).matches() ^ isExclude)
                {
                    // recycle the matcher
                    // cache only the result, not the matcher itself
                    final java.util.regex.Matcher m = this.matcher.reset(text);
                    
                    // we don't cache the matcher but the result which is immutable
                    result = m.toMatchResult();
                    cache.put(text, result);

                    return result;
                }
                else
                {
                    // remember the miss
                    cache.put(text, NULL);
                    return null;
                }
            }

            // ok, we got one, just see if this is NULL or a match
            return result == NULL ? null : result;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence getReplacementText(final RequestData requestData, final int capturingGroupIndex, final Object filterState)
    {
        if (isExclude || matcher == null || capturingGroupIndex == -1)
        {
            return getText(requestData);
        }

        try
        {
            return ((MatchResult) filterState).group(capturingGroupIndex);
        }
        catch (final IndexOutOfBoundsException ioobe)
        {
            final String format = "No matching group %d for input string '%s' and pattern '%s'";
            ThrowableUtils.setMessage(ioobe, String.format(format, capturingGroupIndex, getText(requestData), getPattern()));

            throw ioobe;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("{ type: '");
        sb.append(getTypeCode()).append("', ");
        sb.append("pattern: '").append(getPattern()).append("', ");
        sb.append("isExclude: ").append(isExclude).append(" }");

        return sb.toString();
    }

    /**
     * Returns the filter pattern string.
     */
    public String getPattern()
    {
        return (matcher == null) ? StringUtils.EMPTY : matcher.pattern().pattern();
    }

    /**
     * Whether this filter has an empty pattern.
     */
    public boolean isEmpty()
    {
        return matcher == null;
    }

    /**
     * Whether this filter is an exclude filter.
     */
    public boolean isExclude()
    {
        return isExclude;
    }
}
