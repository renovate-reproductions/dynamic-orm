package com.github.vzakharchenko.dynamic.orm.core.statistic;

import com.github.vzakharchenko.dynamic.orm.core.query.cache.StatisticCacheHolder;
import com.github.vzakharchenko.dynamic.orm.core.transaction.cache.TransactionalCache;
import com.google.common.collect.ImmutableList;
import com.querydsl.sql.RelationalPath;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 *
 */
public class QueryStatisticImpl implements QueryStatistic, QueryStatisticRegistrator {
    private final Set<RelationalPath> qTables = new HashSet<>();

    @Override
    public void register(RelationalPath<?> qTable) {
        qTables.add(qTable);
    }

    @Override
    public void register(Collection<RelationalPath> qTables0) {
        this.qTables.addAll(qTables0);
    }

    @Override
    public List<RelationalPath> getTables() {
        return ImmutableList.copyOf(qTables);
    }

    @Override
    public StatisticCacheHolder get(TransactionalCache transactionalCache,
                                    String sql,
                                    List<? extends Serializable> primaryKeys) {
        String uuid = UUID.randomUUID().toString();
        qTables.forEach(qTable -> transactionalCache
                .putToCache(StringUtils.upperCase(qTable.getTableName()), uuid));
        return new StatisticCacheHolder(primaryKeys, uuid);
    }


}
