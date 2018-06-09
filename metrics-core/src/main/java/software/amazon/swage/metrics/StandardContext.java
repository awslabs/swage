/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not
 * use this file except in compliance with the License. A copy of the License
 * is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package software.amazon.swage.metrics;

import software.amazon.swage.collection.TypedMap;

/**
 * Metric context builder with keys for attributes that is common across many
 * different applications.
 *
 */
public class StandardContext extends ContextData {

    public static final TypedMap.Key<String> MARKETPLACE = TypedMap.key("marketplace", String.class);
    public static final TypedMap.Key<String> SERVICE     = TypedMap.key("serviceName", String.class);
    public static final TypedMap.Key<String> OPERATION   = TypedMap.key("operation", String.class);
    public static final TypedMap.Key<String> PROGRAM     = TypedMap.key("programName", String.class);

    public static StandardContext from(TypedMap data) {
        StandardContext b = new StandardContext();
        data.iterator().forEachRemaining((e) -> b.add(e.getKey(), e));
        return b;
    }

    public static StandardContext withId(String id) {
        StandardContext b = new StandardContext();
        b.add(ID, id);
        return b;
    }

    public StandardContext marketplace(final String marketplace) {
        add(MARKETPLACE, marketplace);
        return this;
    }

    public StandardContext serviceName(final String serviceName) {
        add(SERVICE, serviceName);
        return this;
    }

    public StandardContext operation(final String operation) {
        add(OPERATION, operation);
        return this;
    }

    public StandardContext program(final String program) {
        add(PROGRAM, program);
        return this;
    }

}
