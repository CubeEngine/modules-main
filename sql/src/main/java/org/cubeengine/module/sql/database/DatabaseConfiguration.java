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

import org.cubeengine.reflect.annotations.Comment;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;
import org.jooq.SQLDialect;

/**
 * DatabaseConfiguration have to return their corresponding DatabaseClass.
 */
@SuppressWarnings("all")
public class DatabaseConfiguration extends ReflectedYaml
{
    public boolean logDatabaseQueries = false;
    @Comment("The table prefix to use for all CubeEngine tables")
    public String tablePrefix = "cube_";
    @Comment({"The dialect to use for the CubeEngine database",
            "The following dialects are supported:",
            "H2, MYSQL, POSTGRES, SQLITE",
            "Make sure that the required driver is present on the classpath"})
    public SQLDialect dialect = SQLDialect.SQLITE;
}
