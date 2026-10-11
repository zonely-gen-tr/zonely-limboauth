# Zonely shared account database

This fork uses Zonely's existing `userslist` table. Its primary key is the numeric,
auto-generated `id`. The Minecraft username is stored in `nick`; `subname` is a
website profile field and **must not** be used as the Minecraft username or a
primary key.

Account lookup compares `LOWER(nick)` with the player's case-folded username.
If older data contains duplicate usernames, the lowest numeric `id` is selected.
For example, a pre-existing website row with ID 4 takes precedence over a
duplicate with ID 8. This prevents further logins from targeting the duplicate,
but does **not** remove or merge existing duplicate rows.

By default `main.protect-website-accounts` is enabled. With this protection on,
`/unregister`, `/forceunregister`, and `/premium` do not modify website accounts;
automatic premium authentication also does not clear a website password hash.
Keep this setting enabled whenever Minecraft and Zonely use the same table.
Setting it to false restores LimboAuth's original account deletion and premium
password-clearing behavior and should only be used with a dedicated database.

The player's original website password hash is preserved after a successful
legacy AuthMe password login. Password change commands can still update the
shared password field, so check that the website accepts LimboAuth's bcrypt
format before enabling these commands for players or staff.

Before addressing historical duplicates, back up the database and verify which
row is referenced by website orders, credits, subscriptions and other tables.
Do not delete or merge duplicate accounts automatically. Deploy the updated JAR
to the Velocity proxy after testing with a staging copy of the database.
