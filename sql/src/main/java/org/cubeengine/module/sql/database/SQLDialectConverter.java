/*
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
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
