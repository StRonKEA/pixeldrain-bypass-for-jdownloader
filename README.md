# Pixeldrain Bypass for JDownloader

[Türkçe](#türkçe) · [English](#english)

## Türkçe

Bu, [pixeldrain-bypass.gamedrive.org](https://pixeldrain-bypass.gamedrive.org/) adresindeki userscript'in JDownloader hali. Pixeldrain linkini yapıştırırsın, indirme GameDrive üzerinden gider.

### Kurulum

Hazır dosyayı kullanmak en kolayı. [Releases](https://github.com/StRonKEA/pixeldrain-bypass-for-jdownloader/releases) sayfasından zip'i indir. JDownloader'ı tamamen kapat. Zip'in içindeki `jd` klasörünü, JDownloader kurulumundaki `jd` klasörünün üzerine kopyala. İki dosya kendi yerine gider. Sonra JDownloader'ı aç. Açıkken kopyalanan dosyayı görmez.

Kendin derlemek istersen kaynak buradadır. JDownloader Java 8 ile çalışır, çıktı da 8 olmalı.

```
javac --release 8 -encoding UTF-8 -cp "Core.jar;JDownloader.jar;libs/*" -d out jd/plugins/hoster/PixeldrainBypass.java jd/plugins/decrypter/PixeldrainBypassCrawler.java
```

`Core.jar`, `JDownloader.jar` ve `libs` JDownloader kurulumunun içindedir. Çıkan `PixeldrainBypass.class` dosyasını `jd/plugins/hoster/` altına, `PixeldrainBypassCrawler.class` dosyasını `jd/plugins/decrypter/` altına koy. JDownloader kapalıyken kopyala, sonra aç.

### Kullanım

1. Pixeldrain linkini kopyala. Tek dosya (`/u/`), klasör (`/d/`), albüm (`/l/`) ya da doğrudan bir `cdn.pixeldrain.eu.cc` adresi olur.
2. JDownloader panoda link arıyorsa Bağlantı Toplayıcı'ya kendisi düşer. Düşmezse Bağlantı Toplayıcı sekmesine geç, üstteki **+** simgesine bas, linki yapıştır, Tamam de.
3. Satırda sunucu adı **PixeldrainBypass** yazar. İndirmeyi başlat.

Parça sayısı bu eklentide ayarlanmaz. JDownloader'ın kendi ayarı geçerlidir: **Ayarlar → Genel** içinde dosya başına en fazla bağlantı. İndirme satırına sağ tıklayıp parça sayısını da oradan değiştirebilirsin.

Eklentinin kendi penceresi: **Ayarlar → Eklentiler → PixeldrainBypass**. Orada yalnız kısa bir not var, değiştirilecek bir seçenek yok.

## English

This is the JDownloader version of the userscript at [pixeldrain-bypass.gamedrive.org](https://pixeldrain-bypass.gamedrive.org/). Paste a Pixeldrain link and the download goes through GameDrive.

### Install

The easy way is the ready-made zip on [Releases](https://github.com/StRonKEA/pixeldrain-bypass-for-jdownloader/releases). Quit JDownloader completely. Copy the `jd` folder from the zip onto the `jd` folder in your JDownloader install. The two files land in the right place. Start JDownloader again. It will not see files copied while it is still open.

If you want to compile it yourself, the source is in this repo. JDownloader runs on Java 8, so the class files have to be 8 as well.

```
javac --release 8 -encoding UTF-8 -cp "Core.jar;JDownloader.jar;libs/*" -d out jd/plugins/hoster/PixeldrainBypass.java jd/plugins/decrypter/PixeldrainBypassCrawler.java
```

`Core.jar`, `JDownloader.jar` and `libs` are inside the JDownloader install. Put `PixeldrainBypass.class` in `jd/plugins/hoster/` and `PixeldrainBypassCrawler.class` in `jd/plugins/decrypter/`. Copy them while JDownloader is closed, then start it.

### Use

1. Copy a Pixeldrain link. A single file (`/u/`), a folder (`/d/`), an album (`/l/`), or a direct `cdn.pixeldrain.eu.cc` URL all work.
2. If clipboard watch is on, the link shows up in LinkGrabber by itself. If it doesn't, open the LinkGrabber tab, click the **+** button, paste the link, and confirm.
3. The host column says **PixeldrainBypass**. Start the download.

Chunk count is not a setting in this plugin. Use JDownloader's own one: **Settings → General**, max chunks per file. You can also right-click the download and change the chunks there.

The plugin's own page is **Settings → Plugins → PixeldrainBypass**. It only shows a short note. There is nothing to switch on.
