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

import com.healthmarketscience.sqlbuilder.Condition;
import com.hpe.caf.services.job.exceptions.BadRequestException;
import com.hpe.caf.services.job.exceptions.FilterException;
import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.Node;

public final class RsqlToSqlUtils
{
    private RsqlToSqlUtils()
    {
    }

    public static String convertToSqlSyntax(final String filter) throws BadRequestException
    {
        if (filter == null) {
            return null;
        }
        final RSQLParser rsqlParser = new RSQLParser();
        try {
            final Node rootNode = rsqlParser.parse(filter);
            final Condition filterQueryCondition = RsqlToSqlConverter.convert(rootNode);
            return filterQueryCondition.toString();
        } catch (final RSQLParserException ex) {
            throw new BadRequestException("Unable to parse filter", ex);
        } catch (final FilterException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }
}
