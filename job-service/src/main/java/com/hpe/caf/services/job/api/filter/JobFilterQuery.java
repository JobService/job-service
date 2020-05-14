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

import com.hpe.caf.services.job.exceptions.FilterException;
import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.ComboCondition;
import com.healthmarketscience.sqlbuilder.CommonTableExpression;
import com.healthmarketscience.sqlbuilder.InCondition;
import com.healthmarketscience.sqlbuilder.NotCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.UnaryCondition;
import com.healthmarketscience.sqlbuilder.dbspec.Table;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class JobFilterQuery
{
    private static final Map<String, DbColumn> COLUMN_MAPPINGS;
    private static final DbTable JOB_TABLE;
    private static final DbTable LABEL_TABLE;

    private static final DbColumn JOB_ID;
    private static final DbColumn JOB_NAME;
    private static final DbColumn JOB_PARTITION_ID;
    private static final DbColumn JOB_CREATE_TIME;
    private static final DbColumn JOB_LAST_MODIFIED;
    private static final DbColumn JOB_STATUS;
    private static final DbColumn JOB_PERCENTAGE_COMPLETE;

    private static final DbColumn LABEL_JOB_ID;
    private static final DbColumn LABEL_LABEL;
    private static final DbColumn LABEL_PARTITION_ID;
    private static final DbColumn LABEL_VALUE;

    static {
        JOB_TABLE = new DbTable(new DbSchema(new DbSpec(), ""), "job", "job");
        LABEL_TABLE = new DbTable(new DbSchema(new DbSpec(), ""), "label", "label");

        JOB_ID = JOB_TABLE.addColumn("job_id");
        JOB_NAME = JOB_TABLE.addColumn("name");
        JOB_PARTITION_ID = JOB_TABLE.addColumn("partition_id");
        JOB_CREATE_TIME = JOB_TABLE.addColumn("create_date");
        JOB_LAST_MODIFIED = JOB_TABLE.addColumn("last_update_date");
        JOB_STATUS = JOB_TABLE.addColumn("job_status");
        JOB_PERCENTAGE_COMPLETE = JOB_TABLE.addColumn("percentage_complete");

        LABEL_PARTITION_ID = LABEL_TABLE.addColumn("partition_id");
        LABEL_LABEL = LABEL_TABLE.addColumn("label");
        LABEL_JOB_ID = LABEL_TABLE.addColumn("job_id");
        LABEL_VALUE = LABEL_TABLE.addColumn("value");

        COLUMN_MAPPINGS = new ConcurrentHashMap<>();
        COLUMN_MAPPINGS.put("id", JOB_ID);
        COLUMN_MAPPINGS.put("name", JOB_NAME);
        COLUMN_MAPPINGS.put("createTime", JOB_CREATE_TIME);
        COLUMN_MAPPINGS.put("lastUpdateTime", JOB_LAST_MODIFIED);
        COLUMN_MAPPINGS.put("status", JOB_STATUS);
        COLUMN_MAPPINGS.put("percentageComplete", JOB_PERCENTAGE_COMPLETE);
    }

    public static Condition getQueryStatement(final String key, final ComparisonOperator comparisonOperator, final List<String> args)
    {
        if (key.startsWith("labels.")) {
            final String[] keys = key.split("\\.");
            final Condition con1 = BinaryCondition.equalTo(JOB_PARTITION_ID, LABEL_PARTITION_ID);
            final Condition con2 = BinaryCondition.equalTo(JOB_ID, LABEL_JOB_ID);
            final Condition con3 = convertConditionString(LABEL_LABEL,
                                                          new ComparisonOperator("=="),
                                                          Arrays.asList(keys[1]));
            final Condition con4 = convertConditionString(LABEL_VALUE,
                                                          comparisonOperator,
                                                          args);
            return labelExistsCon(con1, con2, con3, con4);

        }
        return convertConditionString(COLUMN_MAPPINGS.get(key), comparisonOperator, args);
    }

    private static Condition convertConditionString(final DbColumn key, final ComparisonOperator comparisonOperator,
                                                    final List<String> args)
    {

        final Condition condition;
        switch (comparisonOperator.getSymbol()) {
            case "==":
                condition = BinaryCondition.equalTo(key, args.get(0));
                break;
            case "!=":
                condition = BinaryCondition.notEqualTo(key, args.get(0));
                break;
            case "=gt=":
            case ">":
                condition = BinaryCondition.greaterThan(key, args.get(0));
                break;
            case "=ge=":
            case ">=":
                condition = BinaryCondition.greaterThanOrEq(key, args.get(0));
                break;
            case "=lt=":
            case "<":
                condition = BinaryCondition.lessThan(key, args.get(0));
                break;
            case "=le=":
            case "<=":
                condition = BinaryCondition.lessThanOrEq(key, args.get(0));
                break;
            case "=in=":
                condition = new InCondition(key, args);
                break;
            case "=out=":
                condition = new NotCondition(new InCondition(key, args));
                break;
            default:
                throw new FilterException("Unrecognised filter condition: " + comparisonOperator.getSymbol());
        }
        return condition;
    }

    private static Condition labelExistsCon(final Condition... cons)
    {
        final SelectQuery query = new SelectQuery();
        query.addAllColumns();
        final Table table = new CommonTableExpression("label").getTable();
        query.addFromTable(table);
        for (final Condition condition : cons) {
            query.addCondition(condition);
        }
        return new UnaryCondition(UnaryCondition.Op.EXISTS, query);
    }
}










