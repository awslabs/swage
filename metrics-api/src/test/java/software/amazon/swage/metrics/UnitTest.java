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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for Units
 */
class UnitTest {

    @Test
    void forSymbol() throws Exception {
        assertEquals(Unit.NONE, Unit.forSymbol(""));
        assertEquals(Unit.SECOND, Unit.forSymbol("s"));
        assertEquals(Unit.MILLISECOND, Unit.forSymbol("ms"));
        assertEquals(Unit.MICROSECOND, Unit.forSymbol("µs"));
        assertEquals(Unit.PERCENT, Unit.forSymbol("%"));
        assertEquals(Unit.BIT_PER_SEC, Unit.forSymbol("bps"));
        assertEquals(Unit.BYTE_PER_SEC, Unit.forSymbol("Bps"));
    }

}
