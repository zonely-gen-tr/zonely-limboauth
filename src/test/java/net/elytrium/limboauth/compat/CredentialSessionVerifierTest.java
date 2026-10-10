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

package net.elytrium.limboauth.compat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import net.elytrium.limboauth.model.RegisteredPlayer;
import org.junit.jupiter.api.Test;

class CredentialSessionVerifierTest {

  @Test
  void websitePasswordResetRevokesExistingSessionAndAccountDeletionDoesToo() throws Exception {
    String url = "jdbc:h2:mem:password-reset;DB_CLOSE_DELAY=-1";
    try (Connection webConnection = DriverManager.getConnection(url);
        Statement web = webConnection.createStatement();
        JdbcConnectionSource gameConnection = new JdbcConnectionSource(url)) {
      web.execute("CREATE TABLE userslist (subname VARCHAR(32) PRIMARY KEY, nick VARCHAR(32), "
          + "password VARCHAR(255), ip VARCHAR(45), authme_regip VARCHAR(45), "
          + "authme_created_at BIGINT, authme_lastlogin BIGINT, totp VARCHAR(255), "
          + "uuid VARCHAR(64), premiumuuid VARCHAR(64), issuedtime BIGINT)");
      web.execute("INSERT INTO userslist (subname, nick, password) VALUES ('player', 'Player', 'original-hash')");

      Dao<RegisteredPlayer, String> dao = DaoManager.createDao(gameConnection, RegisteredPlayer.class);
      assertTrue(CredentialSessionVerifier.isCurrent(dao, "player", "original-hash"));

      web.execute("UPDATE userslist SET password = 'reset-hash' WHERE subname = 'player'");
      assertFalse(CredentialSessionVerifier.isCurrent(dao, "player", "original-hash"));
      assertTrue(CredentialSessionVerifier.isCurrent(dao, "player", "reset-hash"));

      web.execute("DELETE FROM userslist WHERE subname = 'player'");
      assertFalse(CredentialSessionVerifier.isCurrent(dao, "player", "reset-hash"));
      assertFalse(CredentialSessionVerifier.isCurrent(dao, "player", null));
    }
  }
}
