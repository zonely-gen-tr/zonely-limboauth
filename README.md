# Zonely LimboAuth

[![Build and test](https://github.com/zonely-gen-tr/zonely-limboauth/actions/workflows/build.yml/badge.svg)](https://github.com/zonely-gen-tr/zonely-limboauth/actions/workflows/build.yml)

Velocity için [Elytrium LimboAuth](https://github.com/Elytrium/LimboAuth) tabanlı, Zonely / AuthMe ortak `userslist` tablosuna uyarlanmış giriş eklentisi. Elytrium'un resmi dağıtımı değildir.

## Kurulum

1. [Releases](https://github.com/zonely-gen-tr/zonely-limboauth/releases) bölümünden `limboauth-1.1.15-zonely.1.jar` ve **Velocity sürümünüze ait** LimboAPI JAR'ını indirin.
2. İki JAR'ı Velocity proxy'nin `plugins/` klasörüne koyun. Eski LimboAPI / LimboAuth dosyalarını buradan çıkarın; aynı eklentinin iki sürümünü birlikte yüklemeyin.
3. Proxy'yi başlatıp kapatın. Oluşan `plugins/limboauth/config.yml` içindeki `database` bölümünü düzenleyin.
4. Proxy'yi yeniden başlatın. Logda `limboapi`, `limboauth` ve `Done` görünmeli. Deneme hesabıyla kayıt, yanlış parola, yeniden giriş ve lobiye aktarımı kontrol edin.

Bunlar proxy eklentileridir; Paper / Spigot sunucusunun `plugins/` klasörüne kurulmaz. Lobi adresini ayrıca `velocity.toml` içindeki `[servers]` bölümünde ayarlayın.

## Velocity / LimboAPI eşleşmeleri

| Velocity | İndirilecek LimboAPI | Java | Upstream kaynak |
| --- | --- | --- | --- |
| 3.4.0 | `limboapi-1.1.27-velocity-3.4.0.jar` | 21 | [e638f4d](https://github.com/Elytrium/LimboAPI/commit/e638f4d8e0ef839a36866a3f1f7814f28f1f07fd) |
| 3.5.1 | `limboapi-1.1.27-velocity-3.5.1.jar` | 25 | [013034d](https://github.com/Elytrium/LimboAPI/commit/013034de70f41f611f388e9c73e1a677e41b7870) |
| 4.2.0 | `limboapi-1.1.27-velocity-4.2.0.jar` | 25 | [bfef579](https://github.com/Elytrium/LimboAPI/commit/bfef5799f21d73a80155035c3430f6654aa6b9f8) |

LimboAuth JAR'ı üç sürüm için ortaktır. LimboAPI JAR'ları farklıdır: test edilen proxy'lerin en yüksek protokolleri sırasıyla **774, 776, 777**. Eski `1.1.26` veya en yeni dev-build dosyasını her proxy'ye kurmak protokol kayıt çakışmasına veya `Please update Velocity` hatasına neden olabilir. Eşleşen API dosyasını kurun.

Test kapsamı ve kullanılan Velocity dosyalarının SHA-256 değerleri [TESTING.md](TESTING.md) içindedir. Aynı sürüm adını taşıyan başka proxy derlemeleri farklı iç API / protokol desteği içerebilir; destek belgede doğrulanan derlemeler içindir.

## MySQL / AuthMe / Zonely

AuthMe'nin `DataSource` bölümünü doğrudan yapıştırmayın. LimboAuth'un kendi config dosyasında şu bölüm kullanılır:

```yaml
database:
  storage-type: MYSQL
  hostname: "127.0.0.1:3306"
  user: "YOUR_DB_USER"
  password: "YOUR_DB_PASSWORD"
  database: "YOUR_DB_NAME"
  connection-parameters: "?useSSL=true"
```

TLS parametrelerini kendi MySQL sunucunuza göre ayarlayın. DB parolalarını veya gerçek kullanıcı hash'lerini GitHub'a eklemeyin.

| Alan | `userslist` kolonu |
| --- | --- |
| Küçük harfle oyuncu adı / hesap anahtarı | `subname` |
| Görünen oyuncu adı | `nick` |
| Oyuncu parola özeti | `password` |
| Kayıt IP / tarihi | `authme_regip`, `authme_created_at` |
| Son giriş IP / tarihi | `ip`, `authme_lastlogin` |
| İki aşamalı doğrulama anahtarı | `totp` |

`userslist.password`, **oyuncunun bcrypt hash'idir**. `$2a$`, `$2b$`, PHP uyumlu `$2y$` ve AuthMe `BCRYPT$` biçimleri desteklenir. Yeni kayıtlar bcrypt kullanır. Ayrı bir salt kolonu gerekmez. Diğer eski hash algoritmaları için `main.migration-hash` ayarını inceleyin.

Canlı veritabanına geçmeden önce yedek alın: eklenti eksik `uuid`, `premiumuuid`, `issuedtime` alanlarını ekleyebilir. `authme_*` kolonlarını `limbo_*` olarak yeniden adlandırmayın. Yerel testler izole H2 veritabanında yapılır. `authme_logged` / `authme_session` alanları bu eklentinin oturum durumuyla eşitlenmez.

## Kaynaktan derleme

Java 21 ile:

```bash
./gradlew --no-daemon test shadowJar checkstyleMain checkstyleTest
```

Çıktı: `build/libs/limboauth-1.1.15-zonely.1.jar`.

LimboAPI eşleşmeleri [compatibility/limboapi.json](compatibility/limboapi.json) içinde sabit commit'lere bağlıdır. Git, Node.js ve seçilen eşleşmenin Java sürümüyle:

```bash
node scripts/build_limboapi.mjs 3.4.0
```

JAR ve kaynak ZIP'i `.test-runtime/dist/` altına yazılır. Minecraft protokol verilerini üretmek ilk çalıştırmada uzun sürebilir. Script ortak veri önbelleği kullanır ve Windows'taki veri üreticisinin çıktı tamponunda kilitlenmesini düzeltir; upstream çalışma zamanı kodunu değiştirmez. `LIMBOAPI_SOURCE` mevcut checkout'u, `LIMBOAPI_DATA_CACHE` veri önbelleğini seçebilir.

Windows'ta Gradle test çalışanı Türkçe karakter içeren yolda sınıf bulamazsa ASCII bir çalışma yolu kullanın; örneğin `subst R: "C:\path\to\limbo"` ardından `R:` üzerinde derleyin.

## Yerel entegrasyon testi

`.test-runtime/` içine `velocity-<version>.jar` ve yukarıdaki isimle LimboAPI JAR'ını koyun; bu klasör Git'e eklenmez.

```bash
npm install --prefix .test-runtime/client minecraft-protocol@1.68.0
node scripts/smoke_velocity.mjs 3.4.0 1.1.27-velocity-3.4.0
node scripts/integration_velocity.mjs 3.4.0 1.1.27-velocity-3.4.0 1.21.11
```

`JAVA_BIN` kullanılacak `java` dosyasını seçer. Testler yalnızca localhost'ta geçici proxy / H2 / backend açar. Entegrasyon testi kayıt, yanlış parola reddi, oturum önbelleği dolduktan sonra tekrar giriş ve backend'e aktarımı denetler. Test sonunda geçici sunucular kapanır.

## Lisans / kaynaklar

[Elytrium LimboAuth](https://github.com/Elytrium/LimboAuth), [LimboAPI](https://github.com/Elytrium/LimboAPI) ve katkıda bulunan geliştiricilere teşekkürler. Orijinal telif bildirimleri korunur. Kod [GNU AGPL v3](LICENSE) kapsamındadır. LimboAPI dosyaları upstream tabanlı derlemelerdir; release eklerinde ilgili kaynak ZIP'leri ve SHA-256 dosyası bulunur.
