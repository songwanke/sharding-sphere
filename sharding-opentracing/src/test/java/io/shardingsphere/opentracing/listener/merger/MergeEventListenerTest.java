/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.opentracing.listener.merger;

import io.opentracing.tag.Tags;
import io.shardingsphere.core.jdbc.core.ShardingContext;
import io.shardingsphere.core.jdbc.core.connection.ShardingConnection;
import io.shardingsphere.core.jdbc.core.datasource.ShardingDataSource;
import io.shardingsphere.core.jdbc.core.statement.ShardingPreparedStatement;
import io.shardingsphere.core.jdbc.core.statement.ShardingStatement;
import io.shardingsphere.core.merger.MergeEngine;
import io.shardingsphere.core.merger.dal.DALMergeEngine;
import io.shardingsphere.core.parsing.parser.dialect.mysql.statement.ShowColumnsStatement;
import io.shardingsphere.core.parsing.parser.dialect.mysql.statement.ShowDatabasesStatement;
import io.shardingsphere.opentracing.fixture.ShardingContextBuilder;
import io.shardingsphere.opentracing.listener.BaseEventListenerTest;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public final class MergeEventListenerTest extends BaseEventListenerTest {
    
    private final ShardingContext shardingContext;
    
    private final MergeEngine mergeEngine;
    
    private final ShardingDataSource shardingDataSource;
    
    public MergeEventListenerTest() throws SQLException {
        shardingContext = ShardingContextBuilder.build();
        mergeEngine = new DALMergeEngine(null, null, new ShowDatabasesStatement(), null);
        shardingDataSource = Mockito.mock(ShardingDataSource.class);
        when(shardingDataSource.getShardingContext()).thenReturn(shardingContext);
    }
    
    @Test
    public void assertPreparedStatementRouting() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        ShardingPreparedStatement statement = new ShardingPreparedStatement(new ShardingConnection(shardingDataSource), "show databases");
        Method mergeMethod = ShardingPreparedStatement.class.getDeclaredMethod("merge", MergeEngine.class);
        mergeMethod.setAccessible(true);
        mergeMethod.invoke(statement, mergeEngine);
        assertThat(getTracer().finishedSpans().size(), is(1));
    }
    
    @Test
    public void assertStatementRouting() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ShardingStatement statement = new ShardingStatement(new ShardingConnection(shardingDataSource));
        Method mergeMethod = ShardingStatement.class.getDeclaredMethod("merge", MergeEngine.class);
        mergeMethod.setAccessible(true);
        mergeMethod.invoke(statement, mergeEngine);
        assertThat(getTracer().finishedSpans().size(), is(1));
    }
    
    @Test
    public void assertException() {
        try {
            MergeEngine errorMergeEngine = new DALMergeEngine(null, null, new ShowColumnsStatement(), null);
            ShardingStatement statement = new ShardingStatement(new ShardingConnection(shardingDataSource));
            Method mergeMethod = ShardingStatement.class.getDeclaredMethod("merge", MergeEngine.class);
            mergeMethod.setAccessible(true);
            mergeMethod.invoke(statement, errorMergeEngine);
            // CHECKSTYLE:OFF
        } catch (final Exception ignored) {
            // CHECKSTYLE:ON
        }
        assertThat(getTracer().finishedSpans().size(), is(1));
        assertTrue((Boolean) getTracer().finishedSpans().get(0).tags().get(Tags.ERROR.getKey()));
    }
}