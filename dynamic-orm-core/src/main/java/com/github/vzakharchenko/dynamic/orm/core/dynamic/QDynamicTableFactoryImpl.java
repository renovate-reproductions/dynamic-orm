package com.github.vzakharchenko.dynamic.orm.core.dynamic;

import com.github.vzakharchenko.dynamic.orm.core.OrmQueryFactory;
import com.github.vzakharchenko.dynamic.orm.core.dynamic.schema.SchemaLoader;
import com.github.vzakharchenko.dynamic.orm.core.dynamic.schema.SchemaSaver;
import com.github.vzakharchenko.dynamic.orm.core.dynamic.structure.DynamicStructureSaver;
import com.github.vzakharchenko.dynamic.orm.core.dynamic.structure.DynamicStructureUpdater;
import com.github.vzakharchenko.dynamic.orm.core.dynamic.structure.LiquibaseHolder;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class QDynamicTableFactoryImpl implements QDynamicBuilderContext, AccessDynamicContext {
    private final DataSource dataSource;
    private final Database database;
    private final OrmQueryFactory ormQueryFactory;
    private final Map<String, QDynamicTable> dynamicTableMap = new HashMap<>();
    private final Map<String, SequanceModel> sequenceModelMap = new HashMap<>();
    private final Map<String, ViewModel> viewModelMap = new HashMap<>();
    private DynamicContext dynamicContext;
    private DynamicStructureUpdater dynamicStructureUpdater;

    public QDynamicTableFactoryImpl(OrmQueryFactory ormQueryFactory,
                                    DataSource dataSource) {
        this.dataSource = dataSource;
        this.ormQueryFactory = ormQueryFactory;
        this.dynamicStructureUpdater = new DynamicStructureSaver(dataSource);
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            this.database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        } catch (DatabaseException e) {
            throw new IllegalStateException(e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    @Override
    public DynamicContext getDynamicContext() {
        if (dynamicContext == null) {
            dynamicContext = new DynamicContext(database, ormQueryFactory);
        }
        return dynamicContext;
    }

    @Override
    public QDynamicTable getQDynamicTableByName(String tableName) {
        return getDynamicContext().getQTable(tableName);
    }


    @Override
    public QTableBuilder buildTables(String tableName) {
        return QDynamicTableBuilder.createBuilder(tableName, dataSource, this);
    }


    @Override
    public QSequenceBuilder createSequence(String sequenceName) {
        return new QSequenceBuilderImpl(sequenceName, this);
    }

    @Override
    public QViewBuilder createView(String viewName) {
        return new QViewBuilderImpl(this, viewName);
    }

    @Override
    public void buildSchema() {
        dynamicStructureUpdater.update(LiquibaseHolder.create(dynamicTableMap,
                sequenceModelMap, viewModelMap));
        dynamicContext.registerQTables(dynamicTableMap.values());
        dynamicContext.registerViews(viewModelMap.values());
        dynamicContext.registerSequences(sequenceModelMap);
        clear();
    }

    @Override
    public void saveSchema(SchemaSaver schemaSaver) {
        dynamicContext.saveSchema(schemaSaver);
    }

    @Override
    public void loadSchema(SchemaLoader schemaLoader) {
        dynamicContext.loadSchema(this, schemaLoader);
    }

    @Override
    public void clear() {
        sequenceModelMap.clear();
        dynamicTableMap.clear();
        viewModelMap.clear();
        dynamicStructureUpdater = new DynamicStructureSaver(dataSource);
    }


    @Override
    public void clearCache() {
        getDynamicContext().clear();
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }


    @Override
    public Map<String, QDynamicTable> getContextTables() {
        return dynamicTableMap;
    }

    @Override
    public Map<String, SequanceModel> getContextSequances() {
        return sequenceModelMap;
    }

    @Override
    public Map<String, ViewModel> getViewSequances() {
        return viewModelMap;
    }
}
