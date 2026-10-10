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

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.junit.jupiter.api.Test;

class AuthMePasswordVerifierTest {

  private static final String PASSWORD = "S3cure-password!";
  private static final String BCRYPT_HASH = BCrypt.withDefaults().hashToString(10, PASSWORD.toCharArray());

  @Test
  void acceptsValidBcryptHashesAcrossAuthMePrefixes() {
    assertTrue(AuthMePasswordVerifier.verify(PASSWORD, BCRYPT_HASH));
    assertTrue(AuthMePasswordVerifier.verify(PASSWORD, BCRYPT_HASH.replaceFirst("^\\$2a\\$", "\\$2b\\$")));
    assertTrue(AuthMePasswordVerifier.verify(PASSWORD, BCRYPT_HASH.replaceFirst("^\\$2a\\$", "\\$2y\\$")));
    assertTrue(AuthMePasswordVerifier.verify(PASSWORD, BCRYPT_HASH.replaceFirst("^\\$2a\\$", "BCRYPT\\$")));
  }

  @Test
  void rejectsWrongPasswordsAndMalformedHashes() {
    assertFalse(AuthMePasswordVerifier.verify("bad-password", BCRYPT_HASH));
    assertFalse(AuthMePasswordVerifier.verify(PASSWORD, ""));
    assertFalse(AuthMePasswordVerifier.verify(PASSWORD, null));
    assertFalse(AuthMePasswordVerifier.verify(null, BCRYPT_HASH));
    assertFalse(AuthMePasswordVerifier.verify(PASSWORD, "$SHA$salt$sha256digest"));
    assertFalse(AuthMePasswordVerifier.verify(PASSWORD, "$2a$04$broken"));
    assertFalse(AuthMePasswordVerifier.verify(PASSWORD, "$2a$00$" + "a".repeat(53)));
    assertFalse(AuthMePasswordVerifier.verify("bad-password", BCRYPT_HASH.replaceFirst("^\\$2a\\$", "BCRYPT\\$")));
  }

  @Test
  void verifiesUtf8PasswordsWithoutChangingTheirBytes() {
    String password = "Güçlü-şifre-漢字!";
    String hash = BCrypt.withDefaults().hashToString(4, password.toCharArray());
    assertTrue(AuthMePasswordVerifier.verify(password, hash));
    assertFalse(AuthMePasswordVerifier.verify("Guclu-sifre-漢字!", hash));
  }
}
