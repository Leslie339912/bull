/**
 * Copyright (C) 2019-2020 Expedia, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hotels.beans.generator.bytecode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.testng.Assert.assertNotNull;

import static com.hotels.beans.generator.bytecode.TransformerBytecodeAdapter.DEFAULT_PACKAGE;

import org.mockito.Spy;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.hotels.beans.generator.bytecode.sample.TransformerCtorThrows;
import com.hotels.beans.generator.bytecode.sample.TransformerCtorWithArgs;
import com.hotels.beans.generator.bytecode.sample.TransformerPrivateCtor;
import com.hotels.beans.generator.core.Transformer;
import com.hotels.beans.generator.core.TransformerSpec;
import com.hotels.beans.generator.core.mapping.MappingCodeFactory;
import com.hotels.beans.generator.core.sample.javabean.Destination;
import com.hotels.beans.generator.core.sample.javabean.Source;

import net.openhft.compiler.CachedCompiler;

/**
 * Tests for {@link TransformerBytecodeAdapter}.
 */
public class TransformerBytecodeAdapterTest {
    @Spy
    private final TransformerSpec spec = new TransformerSpec(MappingCodeFactory.getInstance());

    private TransformerBytecodeAdapter underTest;

    @BeforeMethod
    public void setUp() {
        initMocks(this);
        underTest = TransformerBytecodeAdapter.builder()
                .spec(spec)
                .build();
    }

    @Test
    public void shouldCreateANewTransformerWithGivenSpec() {
        // WHEN
        Transformer<Source, Destination> actual =
                underTest.newTransformer(Source.class, Destination.class);

        // THEN
        assertNotNull(actual, "a new Transformer instance is never null");
        then(spec).should().build(Source.class, Destination.class);
    }

    @Test
    public void shouldCreateANewTransformerWithDefaultPackage() {
        // WHEN
        Transformer<Source, Destination> actual =
                underTest.newTransformer(Source.class, Destination.class);

        // THEN
        assertThat(actual.getClass().getName(), startsWith(DEFAULT_PACKAGE));
    }

    @Test
    public void shouldCreateANewTransformerWithGivenPackage() {
        // GIVEN
        String packageName = "foo.bar";
        underTest = TransformerBytecodeAdapter.builder()
                .packageName(packageName)
                .spec(spec)
                .build();

        // WHEN
        Transformer<Source, Destination> actual =
                underTest.newTransformer(Source.class, Destination.class);

        // THEN
        assertThat(actual.getClass().getName(), startsWith(packageName));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void shouldThrowRuntimeExceptionIfCompilerFails() throws Exception {
        // GIVEN
        CachedCompiler compiler = given(mock(CachedCompiler.class).loadFromJava(anyString(), anyString()))
                .willThrow(ClassNotFoundException.class)
                .getMock();
        underTest = TransformerBytecodeAdapter.builder()
                .compiler(compiler)
                .spec(spec)
                .build();

        // WHEN
        underTest.newTransformer(Source.class, Destination.class);
    }

    @Test(dataProvider = "nonCompliantTransformers", expectedExceptions = RuntimeException.class)
    public void shouldThrowRuntimeExceptionIfTransformerIsNonCompliant(final Class<? extends Transformer> trClass)
            throws Exception {
        // GIVEN
        underTest = TransformerBytecodeAdapter.builder()
                .compiler(mockCompilerWith(trClass))
                .spec(spec)
                .build();

        // WHEN
        underTest.newTransformer(Source.class, Destination.class);
    }

    @DataProvider
    public static Object[][] nonCompliantTransformers() {
        return new Object[][]{
                {TransformerPrivateCtor.class},
                {TransformerCtorWithArgs.class},
                {TransformerCtorThrows.class}
        };
    }

    private static CachedCompiler mockCompilerWith(final Class<? extends Transformer> trClass)
            throws ClassNotFoundException {
        return given(mock(CachedCompiler.class).loadFromJava(anyString(), anyString()))
                .willReturn(trClass)
                .getMock();
    }
}
