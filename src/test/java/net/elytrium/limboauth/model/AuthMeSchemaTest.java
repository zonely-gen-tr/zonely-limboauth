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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

class AuthMeSchemaTest {

  @Test
  void originalWebsiteAccountWinsDuplicateNicknameWithoutChangingCreditsOrName() throws Exception {
    String url = "jdbc:h2:mem:authme;DB_CLOSE_DELAY=-1";
    try (Connection connection = DriverManager.getConnection(url);
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE TABLE userslist (id INT AUTO_INCREMENT PRIMARY KEY, "
          + "subname VARCHAR(255) NOT NULL DEFAULT '0', nick VARCHAR(32), "
          + "password VARCHAR(255), credit INT DEFAULT 0, ip VARCHAR(45), authme_regip VARCHAR(45), "
          + "authme_created_at BIGINT, authme_lastlogin BIGINT, totp VARCHAR(255), "
          + "uuid VARCHAR(64), premiumuuid VARCHAR(64), issuedtime BIGINT)");
      statement.execute("INSERT INTO userslist (id, subname, nick, password, credit) "
          + "VALUES (4, '0', 'WhiteEffect', 'site-hash', 98825)");
      statement.execute("INSERT INTO userslist (id, subname, nick, password, credit) "
          + "VALUES (8, 'whiteeffect', 'WhiteEffect', 'duplicate-hash', 0)");
    }
    try (JdbcConnectionSource database = new JdbcConnectionSource(url)) {
      Dao<RegisteredPlayer, Integer> dao = DaoManager.createDao(database, RegisteredPlayer.class);
      RegisteredPlayer player = RegisteredPlayerLookup.findByNickname(dao, "whiteeffect");

      assertNotNull(player);
      assertEquals(4, player.getId());
      assertEquals("WhiteEffect", player.getNickname());
      assertEquals("whiteeffect", player.getLowercaseNickname());
      assertEquals("site-hash", player.getHash());

      player.setLoginDate(123L);
      dao.update(player);
      assertEquals(4, RegisteredPlayerLookup.findByNickname(dao, "WHITEEFFECT").getId());
      assertNull(RegisteredPlayerLookup.findByNickname(dao, "nonexistent"));
    }
    try (Connection connection = DriverManager.getConnection(url);
        Statement statement = connection.createStatement();
        java.sql.ResultSet row = statement.executeQuery(
            "SELECT subname, credit, password, authme_lastlogin FROM userslist WHERE id = 4")) {
      org.junit.jupiter.api.Assertions.assertTrue(row.next());
      assertEquals("0", row.getString(1));
      assertEquals(98825, row.getInt(2));
      assertEquals("site-hash", row.getString(3));
      assertEquals(123L, row.getLong(4));
    }
  }

  @Test
  void newGameRegistrationsUseWebsiteDefaultsAndGeneratedId() throws Exception {
    String url = "jdbc:h2:mem:registration;DB_CLOSE_DELAY=-1";
    try (Connection connection = DriverManager.getConnection(url);
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE TABLE userslist (id INT AUTO_INCREMENT PRIMARY KEY, "
          + "subname VARCHAR(255) NOT NULL DEFAULT '0', nick VARCHAR(32), password VARCHAR(255) NOT NULL, "
          + "ip VARCHAR(45), authme_regip VARCHAR(45), authme_created_at BIGINT, authme_lastlogin BIGINT, "
          + "totp VARCHAR(255), uuid VARCHAR(64), premiumuuid VARCHAR(64), issuedtime BIGINT)");
    }
    try (JdbcConnectionSource database = new JdbcConnectionSource(url)) {
      Dao<RegisteredPlayer, Integer> dao = DaoManager.createDao(database, RegisteredPlayer.class);
      RegisteredPlayer player = new RegisteredPlayer("NewPlayer", "uuid", "127.0.0.1")
          .setHash("new-hash");
      dao.create(player);
      assertNotNull(player.getId());
      assertEquals("NewPlayer", dao.queryForId(player.getId()).getNickname());
      assertEquals(player.getId(), RegisteredPlayerLookup.findByNickname(dao, "newplayer").getId());
      try (Connection connection = DriverManager.getConnection(url);
          Statement statement = connection.createStatement();
          java.sql.ResultSet row = statement.executeQuery(
              "SELECT subname FROM userslist WHERE id = " + player.getId())) {
        org.junit.jupiter.api.Assertions.assertTrue(row.next());
        assertEquals("0", row.getString(1));
      }
    }
  }
}
