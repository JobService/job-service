/*
 * Copyright 2016-2020 Micro Focus or one of its affiliates.
 *
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
 */
package com.hpe.caf.services.job.api.filter;

import com.healthmarketscience.sqlbuilder.ComboCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.hpe.caf.services.job.exceptions.FilterException;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import java.util.Objects;
import java.util.stream.Stream;

public final class RsqlToSqlConverter implements RSQLVisitor<Condition, Void>
{
    public RsqlToSqlConverter()
    {
    }

    @Override
    public Condition visit(final AndNode node, final Void param)
    {
        return createQuery(node);
    }

    @Override
    public Condition visit(final OrNode node, final Void param)
    {
        return createQuery(node);
    }

    @Override
    public Condition visit(final ComparisonNode node, final Void params)
    {
        return createQuery(node);
    }

    private static Condition createQuery(final ComparisonNode comparisonNode)
    {
        final Condition result = JobFilterQuery.getQueryStatement(comparisonNode.getSelector(),
                                                                  comparisonNode.getOperator(),
                                                                  comparisonNode.getArguments());
        return result;
    }

    private static Condition createQuery(final AndNode logicalNode)
    {
        return getStream(logicalNode).reduce(ComboCondition::and).get();
    }

    private static Condition createQuery(final OrNode logicalNode)
    {
        return getStream(logicalNode).reduce(ComboCondition::or).get();
    }

    private static Stream<Condition> getStream(final LogicalNode logicalNode)
    {
        return logicalNode.getChildren()
            .stream()
            .map(RsqlToSqlConverter::createQuery);
    }

    private static Condition createQuery(final Node node)
    {
        if (node instanceof AndNode) {
            return createQuery((AndNode) node);
        }
        if (node instanceof OrNode) {
            return createQuery((OrNode) node);
        }
        if (node instanceof ComparisonNode) {
            return createQuery((ComparisonNode) node);
        }
        throw new FilterException("Unkown node type, node did not match comparision, And or Or node.");
    }
}

