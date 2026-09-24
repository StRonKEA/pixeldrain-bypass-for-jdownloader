# Pixeldrain Bypass for JDownloader

[Türkçe](#türkçe) · [English](#english)

## Türkçe

Bu, [pixeldrain-bypass.gamedrive.org](https://pixeldrain-bypass.gamedrive.org/) adresindeki userscript'in JDownloader hali. Pixeldrain linkini yapıştırırsın, indirme GameDrive üzerinden gider.

### Kurulum

İki Java dosyasını JDownloader'ın kendi jar'larına karşı derle. JDownloader Java 8 ile çalıştığı için çıktı da 8 olmalı.

```
javac --release 8 -encoding UTF-8 -cp "Core.jar;JDownloader.jar;libs/*" -d out jd/plugins/hoster/PixeldrainBypass.java jd/plugins/decrypter/PixeldrainBypassCrawler.java
```

`Core.jar`, `JDownloader.jar` ve `libs` klasörü JDownloader kurulumunun içinde.

Çıkan sınıfları şuraya kopyala:

- `PixeldrainBypass.class` → `jd/plugins/hoster/`
- `PixeldrainBypassCrawler.class` → `jd/plugins/decrypter/`

JDownloader açıksa tamamen kapat. Yeniden açınca eklentiyi görür. Açıkken kopyalanan sınıf yüklenmez.

### Kullanım

1. Pixeldrain linkini kopyala. Tek dosya (`/u/`), klasör (`/d/`), albüm (`/l/`) ya da doğrudan bir `cdn.pixeldrain.eu.cc` adresi olur.
2. JDownloader panoda link arıyorsa Bağlantı Toplayıcı'ya kendisi düşer. Düşmezse Bağlantı Toplayıcı sekmesine geç, üstteki **+** simgesine bas, linki yapıştır, Tamam de.
3. Satırda sunucu adı **PixeldrainBypass** yazar. İndirmeyi başlat.

Parça sayısı bu eklentide ayarlanmaz. JDownloader'ın kendi ayarı geçerlidir: **Ayarlar → Genel** içinde dosya başına en fazla bağlantı. İndirme satırına sağ tıklayıp parça sayısını da oradan değiştirebilirsin.

Eklentinin kendi penceresi: **Ayarlar → Eklentiler → PixeldrainBypass**. Orada yalnız kısa bir not var, değiştirilecek bir seçenek yok.

## English

This is the JDownloader version of the userscript at [pixeldrain-bypass.gamedrive.org](https://pixeldrain-bypass.gamedrive.org/). Paste a Pixeldrain link and the download goes through GameDrive.

### Install

Compile the two Java files against JDownloader's own jars. JDownloader runs on Java 8, so the class files have to be 8 as well.

```
javac --release 8 -encoding UTF-8 -cp "Core.jar;JDownloader.jar;libs/*" -d out jd/plugins/hoster/PixeldrainBypass.java jd/plugins/decrypter/PixeldrainBypassCrawler.java
```

`Core.jar`, `JDownloader.jar` and the `libs` folder are inside the JDownloader install.

Copy the classes here:

- `PixeldrainBypass.class` → `jd/plugins/hoster/`
- `PixeldrainBypassCrawler.class` → `jd/plugins/decrypter/`

If JDownloader is open, quit it fully. It only picks up the classes on the next start.

### Use

1. Copy a Pixeldrain link. A single file (`/u/`), a folder (`/d/`), an album (`/l/`), or a direct `cdn.pixeldrain.eu.cc` URL all work.
2. If clipboard watch is on, the link shows up in LinkGrabber by itself. If it doesn't, open the LinkGrabber tab, click the **+** button, paste the link, and confirm.
3. The host column says **PixeldrainBypass**. Start the download.

Chunk count is not a setting in this plugin. Use JDownloader's own one: **Settings → General**, max chunks per file. You can also right-click the download and change the chunks there.

The plugin's own page is **Settings → Plugins → PixeldrainBypass**. It only shows a short note. There is nothing to switch on.
