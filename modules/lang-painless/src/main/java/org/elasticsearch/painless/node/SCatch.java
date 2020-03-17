/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.painless.node;

import org.elasticsearch.painless.Location;
import org.elasticsearch.painless.Scope;
import org.elasticsearch.painless.ir.CatchNode;
import org.elasticsearch.painless.ir.ClassNode;
import org.elasticsearch.painless.lookup.PainlessLookupUtility;
import org.elasticsearch.painless.symbol.ScriptRoot;

import java.util.Objects;

/**
 * Represents a catch block as part of a try-catch block.
 */
public final class SCatch extends AStatement {

    private final DType baseException;
    private final SDeclaration declaration;
    private final SBlock block;

    public SCatch(Location location, DType baseException, SDeclaration declaration, SBlock block) {
        super(location);

        this.baseException = Objects.requireNonNull(baseException);
        this.declaration = Objects.requireNonNull(declaration);
        this.block = block;
    }

    @Override
    Output analyze(ScriptRoot scriptRoot, Scope scope, Input input) {
        this.input = input;
        output = new Output();

        declaration.analyze(scriptRoot, scope, new Input());

        Class<?> baseType = baseException.resolveType(scriptRoot.getPainlessLookup()).getType();
        Class<?> type = scope.getVariable(location, declaration.name).getType();

        if (baseType.isAssignableFrom(type) == false) {
            throw createError(new ClassCastException(
                    "cannot cast from [" + PainlessLookupUtility.typeToCanonicalTypeName(type) + "] " +
                    "to [" + PainlessLookupUtility.typeToCanonicalTypeName(baseType) + "]"));
        }

        if (block != null) {
            Input blockInput = new Input();
            blockInput.lastSource = input.lastSource;
            blockInput.inLoop = input.inLoop;
            blockInput.lastLoop = input.lastLoop;
            Output blockOutput = block.analyze(scriptRoot, scope, blockInput);

            output.methodEscape = blockOutput.methodEscape;
            output.loopEscape = blockOutput.loopEscape;
            output.allEscape = blockOutput.allEscape;
            output.anyContinue = blockOutput.anyContinue;
            output.anyBreak = blockOutput.anyBreak;
            output.statementCount = blockOutput.statementCount;
        }

        return output;
    }

    @Override
    CatchNode write(ClassNode classNode) {
        CatchNode catchNode = new CatchNode();

        catchNode.setDeclarationNode(declaration.write(classNode));
        catchNode.setBlockNode(block == null ? null : block.write(classNode));

        catchNode.setLocation(location);

        return catchNode;
    }

    @Override
    public String toString() {
        return singleLineToString(baseException, declaration, block);
    }
}
