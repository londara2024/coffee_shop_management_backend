package com.aura.coffeeshop.common.persistence;

import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext;
import org.hibernate.persister.entity.AbstractEntityPersister;

import java.lang.reflect.Member;
import java.util.EnumSet;

/**
 * Generates 5-character IDs by calling the database function {@code short_id(table)} (V1 migration).
 *
 * <p>With only 62^5 (~916 million) possible IDs, random collisions become likely once a table holds tens of
 * thousands of rows, so the function checks the target table and retries until it finds an unused ID.
 * Raw SQL inserts use the same function as the column default, so both paths share one implementation.
 */
public class ShortIdGenerator implements BeforeExecutionGenerator {

    public ShortIdGenerator(ShortId config, Member idMember, CustomIdGeneratorCreationContext context) {
        // no configuration
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue,
                           EventType eventType) {
        if (currentValue != null) {
            return currentValue; // an explicitly assigned ID is kept
        }
        String table = ((AbstractEntityPersister) session.getEntityPersister(null, owner)).getTableName();
        var query = session.createNativeQuery("select short_id(:table)", String.class);
        query.setParameter("table", table);
        // never auto-flush from inside an insert
        query.setHibernateFlushMode(FlushMode.MANUAL);
        return query.getSingleResult();
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EventTypeSets.INSERT_ONLY;
    }
}
