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
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import java.util.stream.StreamSupport;

public enum RsqlToSqlConverter implements RSQLVisitor<Condition, Void>
{
    INSTANCE;

    @Override
    public Condition visit(final AndNode node, final Void param)
    {
        return ComboCondition.and(
            StreamSupport.stream(node.spliterator(), false)
                .map(RsqlToSqlConverter::convert)
                .toArray()
        );
    }

    @Override
    public Condition visit(final OrNode node, final Void param)
    {
        return ComboCondition.or(
            StreamSupport.stream(node.spliterator(), false)
                .map(RsqlToSqlConverter::convert)
                .toArray()
        );
    }

    @Override
    public Condition visit(final ComparisonNode node, final Void params)
    {
        return JobFilterQuery.getQueryStatement(node.getSelector(), node.getOperator(), node.getArguments());
    }

    public static Condition convert(final Node node)
    {
        return node.accept(INSTANCE);
    }
}
