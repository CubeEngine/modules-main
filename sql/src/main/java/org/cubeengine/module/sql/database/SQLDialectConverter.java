package org.cubeengine.module.sql.database;

import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.converter.SimpleConverter;
import org.cubeengine.converter.node.Node;
import org.cubeengine.converter.node.StringNode;
import org.jooq.SQLDialect;


public class SQLDialectConverter extends SimpleConverter<SQLDialect> {
    @Override
    public Node toNode(SQLDialect sqlDialect) throws ConversionException {

        filterAllowedDialect(sqlDialect, null);

        return StringNode.of(sqlDialect.name());
    }

    @Override
    public SQLDialect fromNode(Node node) throws ConversionException {

        SQLDialect sqlDialect = SQLDialect.valueOf(node.asText());
        filterAllowedDialect(sqlDialect, node);
        return sqlDialect;
    }

    private void filterAllowedDialect(SQLDialect sqlDialect, Node node) throws ConversionException {
        switch (sqlDialect) {

            case H2:
            case MYSQL:
            case POSTGRES:
            case SQLITE:
                break;
            default:
                throw ConversionException.of(this, node, "Enum value not found!");
        }
    }
}
