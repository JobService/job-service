/*
 * Copyright 2016-2024 Open Text.
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
import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class CustomRSQLVisitorTest
{
    @Test
    public void twoLabelValueConditions()
    {
        final String filter = "labels.tag==tag.1.2 and labels.owner==anthony.mcgreevy@microfocus.com";
        final RSQLParser rsqlParser = new RSQLParser();
        final Node rootNode = rsqlParser.parse(filter);
        final Condition filterQueryCondition = RsqlToSqlConverter.convert(rootNode);
        assertEquals("((EXISTS (SELECT * FROM label WHERE ((job.partition_id = label.partition_id) "
            + "AND (job.job_id = label.job_id) AND (label.label = 'tag') "
            + "AND (label.value = 'tag.1.2')))) "
            + "AND (EXISTS (SELECT * FROM label WHERE ((job.partition_id = label.partition_id) "
            + "AND (job.job_id = label.job_id) AND (label.label = 'owner') "
            + "AND (label.value = 'anthony.mcgreevy@microfocus.com')))))",
                     filterQueryCondition.toString());
    }

    @Test
    public void labelValueCondition()
    {
        final String filter = "labels.label1==value";
        final RSQLParser rsqlParser = new RSQLParser();
        final Node rootNode = rsqlParser.parse(filter);
        final Condition filterQueryCondition = RsqlToSqlConverter.convert(rootNode);
        assertEquals("(EXISTS (SELECT * FROM label WHERE ((job.partition_id = label.partition_id) "
            + "AND (job.job_id = label.job_id) AND (label.label = 'label1') "
            + "AND (label.value = 'value'))))",
                     filterQueryCondition.toString());
    }

    @Test
    public void labelValueConditionOrAlternative()
    {
        final String filter = "labels.tag==tag.1.2 or labels.owner==anthony.mcgreevy@microfocus.com";
        final RSQLParser rsqlParser = new RSQLParser();
        final Node rootNode = rsqlParser.parse(filter);
        final Condition filterQueryCondition = RsqlToSqlConverter.convert(rootNode);
        assertEquals("((EXISTS (SELECT * FROM label WHERE ((job.partition_id = label.partition_id) "
            + "AND (job.job_id = label.job_id) AND (label.label = 'tag') "
            + "AND (label.value = 'tag.1.2')))) "
            + "OR (EXISTS (SELECT * FROM label WHERE ((job.partition_id = label.partition_id) "
            + "AND (job.job_id = label.job_id) AND (label.label = 'owner') "
            + "AND (label.value = 'anthony.mcgreevy@microfocus.com')))))",
                     filterQueryCondition.toString());
    }

    @Test
    public void multipleLabelConditionWithNestedOrCondition()
    {
        final String filter = "(labels.tag==tag.1.2 or labels.tag==tag.1.3) and labels.tagged==true";
        final RSQLParser rsqlParser = new RSQLParser();
        final Node rootNode = rsqlParser.parse(filter);
        final Condition filterQueryCondition = RsqlToSqlConverter.convert(rootNode);
        assertEquals("(((EXISTS (SELECT * FROM label WHERE ((job.partition_id = label.partition_id) "
            + "AND (job.job_id = label.job_id) "
            + "AND (label.label = 'tag') "
            + "AND (label.value = 'tag.1.2')))) "
            + "OR (EXISTS (SELECT * FROM label WHERE ((job.partition_id = label.partition_id) "
            + "AND (job.job_id = label.job_id) AND (label.label = 'tag') "
            + "AND (label.value = 'tag.1.3'))))) "
            + "AND (EXISTS (SELECT * FROM label WHERE ((job.partition_id = label.partition_id) "
            + "AND (job.job_id = label.job_id) AND (label.label = 'tagged') AND (label.value = 'true')))))",
                     filterQueryCondition.toString());
    }

    @Test
    public void filterOnJobId()
    {
        final String filter = "id==2";
        final RSQLParser rsqlParser = new RSQLParser();
        final Node rootNode = rsqlParser.parse(filter);
        final Condition filterQueryCondition = RsqlToSqlConverter.convert(rootNode);
        assertEquals("(job.job_id = '2')", filterQueryCondition.toString());
    }

    @Test
    public void filterOnJobName()
    {
        final String filter = "name==Job_6f99d48c-2a9e-4257-b18d-2c8d039dfa52";
        final RSQLParser rsqlParser = new RSQLParser();
        final Node rootNode = rsqlParser.parse(filter);
        final Condition filterQueryCondition = RsqlToSqlConverter.convert(rootNode);
        assertEquals("(job.name = 'Job_6f99d48c-2a9e-4257-b18d-2c8d039dfa52')", filterQueryCondition.toString());
    }

    @Test
    public void filterOnStatus()
    {
        final String filter = "status==Active or status==Waiting";
        final RSQLParser rsqlParser = new RSQLParser();
        final Node rootNode = rsqlParser.parse(filter);
        final Condition filterQueryCondition = RsqlToSqlConverter.convert(rootNode);
        assertEquals("((job.status = 'Active') OR (job.status = 'Waiting'))", filterQueryCondition.toString());
    }

    @Test
    public void filterOnPercentageCompleteGreaterThan()
    {
        final String filter = "percentageComplete=gt=50";
        final RSQLParser rsqlParser = new RSQLParser();
        final Node rootNode = rsqlParser.parse(filter);
        final Condition filterQueryCondition = RsqlToSqlConverter.convert(rootNode);
        assertEquals("(job.percentage_complete > 50.0)", filterQueryCondition.toString());
    }

    @Test
    public void filterOnPercentageCompleteGreaterThanOrEqualTo()
    {
        final String filter = "percentageComplete=ge=50";
        final RSQLParser rsqlParser = new RSQLParser();
        final Node rootNode = rsqlParser.parse(filter);
        final Condition filterQueryCondition = RsqlToSqlConverter.convert(rootNode);
        assertEquals("(job.percentage_complete >= 50.0)", filterQueryCondition.toString());
    }

    @Test
    public void filterOnPercentageCompleteLessThanAndLessThanEqualTo()
    {
        final String filter = "percentageComplete=lt=50 and percentageComplete=le=80.5";
        final RSQLParser rsqlParser = new RSQLParser();
        final Node rootNode = rsqlParser.parse(filter);
        final Condition filterQueryCondition = RsqlToSqlConverter.convert(rootNode);
        assertEquals("((job.percentage_complete < 50.0) AND (job.percentage_complete <= 80.5))", filterQueryCondition.toString());
    }

    @Test
    public void filterOnStatusInGroup()
    {
        final String filter = "status=in=(Active,Waiting,Failed)";
        final RSQLParser rsqlParser = new RSQLParser();
        final Node rootNode = rsqlParser.parse(filter);
        final Condition filterQueryCondition = RsqlToSqlConverter.convert(rootNode);
        assertEquals("(job.status IN ('Active','Waiting','Failed') )", filterQueryCondition.toString());
    }

    @Test
    public void filterOnStatusNotInGroup()
    {
        final String filter = "status=out=(Active,Waiting,Complete)";
        final RSQLParser rsqlParser = new RSQLParser();
        final Node rootNode = rsqlParser.parse(filter);
        final Condition filterQueryCondition = RsqlToSqlConverter.convert(rootNode);
        assertEquals("(NOT (job.status IN ('Active','Waiting','Complete') ))", filterQueryCondition.toString());
    }

    @Test
    public void filterOnNameStartsWith()
    {
        final String filter = "name==batchjob.*";
        final RSQLParser rsqlParser = new RSQLParser();
        final Node rootNode = rsqlParser.parse(filter);
        final Condition filterQueryCondition = RsqlToSqlConverter.convert(rootNode);
        assertEquals("(job.name LIKE 'batchjob.%')", filterQueryCondition.toString());
    }

    @Test
    public void filterOnNameEndsWith()
    {
        final String filter = "name==*-batchjob";
        final RSQLParser rsqlParser = new RSQLParser();
        final Node rootNode = rsqlParser.parse(filter);
        final Condition filterQueryCondition = RsqlToSqlConverter.convert(rootNode);
        assertEquals("(job.name LIKE '%-batchjob')", filterQueryCondition.toString());
    }

    @Test
    public void filterOnNameNotEquals()
    {
        final String filter = "name!=batchjob";
        final RSQLParser rsqlParser = new RSQLParser();
        final Node rootNode = rsqlParser.parse(filter);
        final Condition filterQueryCondition = RsqlToSqlConverter.convert(rootNode);
        assertEquals("(job.name <> 'batchjob')", filterQueryCondition.toString());
    }
}
