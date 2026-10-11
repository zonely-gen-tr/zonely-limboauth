# Zonely LimboAuth 1.1.15-zonely.3

### Ortak web hesabı ve yinelenen kayıt düzeltmesi

- Hesap anahtarı `userslist.id`, oyuncu adı `nick` olarak eşlenir. Web profil alanı `subname` değiştirilmez.
- Büyük/küçük harf farkı ve eski yinelenen kayıtlar için en küçük ID'li hesap seçilir. Mevcut yinelenen kayıtlar otomatik silinmez veya birleştirilmez.
- Başarılı eski AuthMe SHA girişlerinde web sitesinin mevcut parola özeti korunur; kredi alanına yazılmaz.
- `main.protect-website-accounts: true` varsayılandır. `/unregister`, `/forceunregister` ve `/premium` engellenir; otomatik premium giriş web parolasını temizlemez.
- Parola değiştirme komutları bcrypt yazar; web sitesinin bu biçimi desteklediğini doğrulayın. Ayrıntılar: [ortak veritabanı rehberi](https://github.com/zonely-gen-tr/zonely-limboauth/blob/master/docs/ZONELY_SHARED_DATABASE.md).
- Hesap eşleme, kredi/parola koruma ve oturum iptali dahil 6 otomatik test geçer. Önceki sürümün proxy entegrasyon kapsamı [TESTING.md](https://github.com/zonely-gen-tr/zonely-limboauth/blob/master/TESTING.md) içinde ayrıca belirtilir.

### Web password reset / session invalidation

- Changes to the shared `userslist.password` column now invalidate cached logins, even when the player reconnects from the same IP.
- Connected players are disconnected after an external password reset or account deletion; checks run every 5 seconds by default (`main.password-change-check-millis`).
- A password changed while the player is in the auth limbo can no longer be bypassed with the previously loaded hash. New credentials must be used to log in.
- The action is automatic when the website and LimboAuth use the same database. The website must persist the updated bcrypt hash to `userslist.password`.
- Existing LimboAPI compatibility matrix and source archives continue to apply.

Velocity 3.4.0 / 3.5.1 için LimboAPI protokol uyuşmazlığını sürüme özel API dosyalarıyla çözer. LimboAuth JAR'ı ortaktır; Velocity sürümünüze ait **tek** LimboAPI JAR'ını kullanın.

| Velocity | LimboAPI dosyası | Java |
| --- | --- | --- |
| 3.4.0 | `limboapi-1.1.27-velocity-3.4.0.jar` | 21 |
| 3.5.1 | `limboapi-1.1.27-velocity-3.5.1.jar` | 25 |
| 4.2.0 | `limboapi-1.1.27-velocity-4.2.0.jar` | 25 |

- AuthMe / Zonely `userslist` alanları: `id`, `nick`, `password`, `authme_regip`, `authme_created_at`, `authme_lastlogin`, `ip`, `totp`. `subname` web profiline aittir.
- Bcrypt `$2a$`, `$2b$`, `$2y$` ve AuthMe `BCRYPT$` desteği; bozuk hash'ler reddedilir.
- H2 ilk başlangıçta yinelenen kolon hatası düzeltildi.
- Eksik config alanları güncellenirken `{PRFX}` mesaj ayrıştırma hatası düzeltildi.
- Yerel proxy başlangıcı ve kayıt / yanlış parola / yeniden giriş / backend aktarımı için otomatik test script'leri eklendi.

Test edilen Minecraft sürümleri, proxy SHA-256 değerleri ve kapsam için [TESTING.md](https://github.com/zonely-gen-tr/zonely-limboauth/blob/master/TESTING.md); MySQL ayarı ve kurulum için [README](https://github.com/zonely-gen-tr/zonely-limboauth#readme).

LimboAPI dosyaları Elytrium upstream commit'lerinden gelir. AGPL-3.0 kaynak ZIP'leri ve `SHA256SUMS.txt` release eklerinde bulunur. `zonely-plugins-velocity-*.zip` paketleri ilgili iki eklentiyi ve kaynakları birlikte içerir; Velocity sunucusu ayrıca kurulmalıdır. MySQL kimlik bilgisi içermez.
