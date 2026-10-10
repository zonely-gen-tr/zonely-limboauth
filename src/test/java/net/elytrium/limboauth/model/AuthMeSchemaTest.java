/*
 * Copyright (C) 2021 - 2025 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limboauth.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

class AuthMeSchemaTest {

  @Test
  void readsSharedAuthMeColumnsWithoutRenamingThem() throws Exception {
    try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:authme;DB_CLOSE_DELAY=-1");
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE TABLE userslist (id BIGINT AUTO_INCREMENT, subname VARCHAR(32) PRIMARY KEY, "
          + "nick VARCHAR(32), password VARCHAR(255), ip VARCHAR(45), authme_regip VARCHAR(45), "
          + "authme_created_at BIGINT, authme_lastlogin BIGINT, totp VARCHAR(255), "
          + "uuid VARCHAR(64), premiumuuid VARCHAR(64), issuedtime BIGINT)");
      statement.execute("INSERT INTO userslist (subname, nick, password) "
          + "VALUES ('zonelytester', 'ZonelyTester', 'test-hash')");
    }
    try (JdbcConnectionSource database = new JdbcConnectionSource("jdbc:h2:mem:authme;DB_CLOSE_DELAY=-1")) {
      Dao<RegisteredPlayer, String> dao = DaoManager.createDao(database, RegisteredPlayer.class);
      RegisteredPlayer player = dao.queryForId("zonelytester");

      assertNotNull(player);
      assertEquals("ZonelyTester", player.getNickname());
      assertEquals("test-hash", player.getHash());
    }
  }
}
