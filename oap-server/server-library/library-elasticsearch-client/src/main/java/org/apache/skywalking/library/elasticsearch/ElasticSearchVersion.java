/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.skywalking.library.elasticsearch;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ElasticSearchVersion implements Comparable<ElasticSearchVersion> {
    public static final ElasticSearchVersion UNKNOWN = new ElasticSearchVersion("UNKNOWN", -1, -1);

    public static final ElasticSearchVersion
        ES_V6_0 = new ElasticSearchVersion("ElasticSearch", 6, 0);
    public static final ElasticSearchVersion
        ES_V7_0 = new ElasticSearchVersion("ElasticSearch", 7, 0);
    public static final ElasticSearchVersion
        ES_V7_8 = new ElasticSearchVersion("ElasticSearch", 7, 8);
    public static final ElasticSearchVersion
        ES_V8_0 = new ElasticSearchVersion("ElasticSearch", 8, 0);

    public static final ElasticSearchVersion
        OS_V1 = new ElasticSearchVersion("OpenSearch", 1, 0);

    private final String distribution;
    private final int major;
    private final int minor;

    @Override
    public int compareTo(final ElasticSearchVersion o) {
        if (distribution.equalsIgnoreCase(o.distribution)) {
            if (major != o.major) {
                return Integer.compare(major, o.major);
            }
            return Integer.compare(minor, o.minor);
        }
        return -1;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ElasticSearchVersion that = (ElasticSearchVersion) o;
        return major == that.major && minor == that.minor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor);
    }

    @Override
    public String toString() {
        return distribution + " " + major + "." + minor;
    }

    private static final Pattern REGEX = Pattern.compile("(\\d+)\\.(\\d+).*");

    public static ElasticSearchVersion from(String distribution, String version) {
        final Matcher matcher = REGEX.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Failed to parse version: " + version);
        }
        final int major = Integer.parseInt(matcher.group(1));
        final int minor = Integer.parseInt(matcher.group(2));
        return new ElasticSearchVersion(distribution, major, minor);
    }
}
