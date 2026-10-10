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

import at.favre.lib.crypto.bcrypt.BCrypt;
import java.nio.charset.StandardCharsets;

/** Verifies bcrypt passwords stored in the shared AuthMe/Zonely userslist.password column. */
public final class AuthMePasswordVerifier {

  private static final BCrypt.Verifyer VERIFIER = BCrypt.verifyer();

  private AuthMePasswordVerifier() {
  }

  public static boolean verify(String password, String storedHash) {
    if (password == null || storedHash == null || storedHash.isBlank()) {
      return false;
    }

    String hash = storedHash;
    if (hash.startsWith("BCRYPT$")) {
      hash = "$2a$" + hash.substring("BCRYPT$".length());
    }

    // AuthMe can store PHP-compatible $2y$ hashes; normalize the marker for the verifier.
    if (hash.startsWith("$2y$")) {
      hash = "$2a$" + hash.substring(4);
    }

    if (!(hash.startsWith("$2a$") || hash.startsWith("$2b$")) || hash.length() != 60) {
      return false;
    }

    try {
      return VERIFIER.verify(password.getBytes(StandardCharsets.UTF_8),
          hash.getBytes(StandardCharsets.UTF_8)).verified;
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }
}
