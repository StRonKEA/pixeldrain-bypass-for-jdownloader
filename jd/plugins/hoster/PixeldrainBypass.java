package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import org.appwork.storage.TypeRef;

@HostPlugin(revision = "$Revision: 31 $", interfaceVersion = 3, names = { "PixeldrainBypass" }, urls = { "pixeldrainbypass://(.+)|https?://cdn[0-9]*\\.pixeldrain\\.eu\\.cc/.+" })
@SuppressWarnings("unchecked")
public class PixeldrainBypass extends PluginForHost {

    private static final List<String> PROXIES = new CopyOnWriteArrayList<String>();
    private static volatile long lastProxyFetch = 0;
    private static final long CACHE_TTL = 24 * 60 * 60 * 1000L;

    public static boolean isTurkish() {
        try {
            String lang = java.util.Locale.getDefault().getLanguage();
            if (lang != null && lang.equalsIgnoreCase("tr")) {
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public PixeldrainBypass(PluginWrapper wrapper) {
        super(wrapper);
        boolean tr = isTurkish();
        ConfigContainer config = getConfig();
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, "Pixeldrain Download Bypass Enhanced"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, tr
            ? "https://pixeldrain-bypass.gamedrive.org/ adresi için yapılan resmi userscript'in JDownloader uyarlaması."
            : "JDownloader port of the official userscript for https://pixeldrain-bypass.gamedrive.org/"));
    }

    @Override
    public String rewriteHost(String host) {
        if (host != null && (host.equalsIgnoreCase("pixeldrain.bypass") || host.equalsIgnoreCase("cdn.pixeldrain.eu.cc") || host.equalsIgnoreCase("pixeldrain.com (bypass)") || host.equalsIgnoreCase("PixeldrainDownloadBypassEnhanced"))) {
            return "PixeldrainBypass";
        }
        return super.rewriteHost(host);
    }

    @Override
    public String getAGBLink() {
        return "https://pixeldrain-bypass.gamedrive.org";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public boolean isResumeable(DownloadLink link, Account account) {
        return true;
    }

    private String getFID(DownloadLink link) {
        String url = link.getPluginPatternMatcher();
        if (url == null) return "";
        if (url.startsWith("pixeldrainbypass://")) {
            return url.substring("pixeldrainbypass://".length());
        }
        Regex m = new Regex(url, "https?://cdn[0-9]*\\.pixeldrain\\.eu\\.cc/(.+)");
        if (m.patternFind()) {
            String path = m.getMatch(0);
            int qIdx = path.indexOf('?');
            if (qIdx != -1) path = path.substring(0, qIdx);
            int hIdx = path.indexOf('#');
            if (hIdx != -1) path = path.substring(0, hIdx);
            if (path.startsWith("api/file/")) {
                path = path.substring("api/file/".length());
            } else if (path.startsWith("api/filesystem/")) {
                path = "d/" + path.substring("api/filesystem/".length());
            }
            return path;
        }
        return url;
    }

    private String normalizeProxy(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "https://" + s;
        }
        if (!s.endsWith("/")) {
            s = s + "/";
        }
        return s;
    }

    private void addProxy(List<String> list, Object item) {
        if (item == null) return;
        String normalized = normalizeProxy(item.toString());
        if (normalized != null && !list.contains(normalized)) {
            list.add(normalized);
        }
    }

    private synchronized void updateProxyList(Browser br) {
        long now = System.currentTimeMillis();
        if (!PROXIES.isEmpty() && (now - lastProxyFetch) < CACHE_TTL) {
            return;
        }
        try {
            Browser pbr = br.cloneBrowser();
            pbr.setFollowRedirects(true);
            pbr.setConnectTimeout(6000);
            pbr.setReadTimeout(8000);
            pbr.getPage("https://pixeldrain-bypass.gamedrive.org/api/proxy.json");
            String json = pbr.getRequest().getHtmlCode();
            List<String> list = new ArrayList<String>();
            if (json != null && json.trim().startsWith("[")) {
                List<Object> arr = restoreFromString(json, TypeRef.LIST);
                if (arr != null) {
                    for (Object item : arr) {
                        addProxy(list, item);
                    }
                }
            } else {
                Map<String, Object> map = restoreFromString(json, TypeRef.MAP);
                Object proxiesObj = map.get("proxies");
                if (proxiesObj instanceof List) {
                    for (Object item : (List<?>) proxiesObj) {
                        addProxy(list, item);
                    }
                } else if (map.get("proxy") instanceof String) {
                    addProxy(list, map.get("proxy"));
                }
            }
            if (!list.isEmpty()) {
                PROXIES.clear();
                PROXIES.addAll(list);
                lastProxyFetch = now;
                logger.info("Updated GameDrive bypass proxy list: " + PROXIES);
            }
        } catch (Exception e) {
            logger.log(e);
        }
        if (PROXIES.isEmpty()) {
            PROXIES.add("https://cdn.pixeldrain.eu.cc/");
        }
    }

    private String getActiveProxy(Browser br) {
        updateProxyList(br);
        if (!PROXIES.isEmpty()) {
            int index = (int) (Math.random() * PROXIES.size());
            return PROXIES.get(index);
        }
        return "https://cdn.pixeldrain.eu.cc/";
    }

    private String resolveDirectCdnUrl(DownloadLink link, String fid) throws Exception {
        if (fid.startsWith("http://") || fid.startsWith("https://")) {
            return fid;
        }

        String cached = link.getStringProperty("direct_cdn_url", null);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        String proxy = getActiveProxy(this.br);
        String initialUrl = proxy + fid;
        if (fid.startsWith("d/") || fid.startsWith("ds/") || fid.startsWith("zip/") || fid.startsWith("dzip/")) {
            return initialUrl;
        }

        try {
            Browser rbr = createNewBrowserInstance();
            rbr.setFollowRedirects(false);
            rbr.setConnectTimeout(8000);
            rbr.setReadTimeout(8000);
            rbr.getPage(initialUrl);
            String loc = rbr.getRedirectLocation();
            if (loc != null && !loc.isEmpty()) {
                link.setProperty("direct_cdn_url", loc);
                return loc;
            }
        } catch (Exception e) {
            logger.log(e);
        }
        return initialUrl;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        link.setHost("PixeldrainBypass");
        if (link.getFinalFileName() != null && link.getDownloadSize() > 0) {
            return AvailableStatus.TRUE;
        }

        String fid = getFID(link);
        if (fid == null || fid.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        // Tek dosyalık klasör: userscript ile aynı ds/ adresi
        if (fid.startsWith("ds/")) {
            if (link.getFinalFileName() == null) {
                link.setFinalFileName(fid.substring("ds/".length()));
            }
            return AvailableStatus.TRUE;
        }

        // zip/ veya dzip/ ise dosya adını türet
        if (fid.startsWith("zip/") || fid.startsWith("dzip/")) {
            String zipId = fid.substring(fid.indexOf('/') + 1);
            if (link.getFinalFileName() == null) {
                link.setFinalFileName(zipId + ".zip");
            }
            return AvailableStatus.TRUE;
        }

        // d/<dirId>/<encodedName> ise dosya adını path'ten türet
        if (fid.startsWith("d/")) {
            String[] parts = fid.split("/", 3);
            if (parts.length >= 3 && link.getFinalFileName() == null) {
                try {
                    String decoded = java.net.URLDecoder.decode(parts[2], "UTF-8");
                    link.setFinalFileName(decoded);
                } catch (Exception ignored) {
                    link.setFinalFileName(parts[2]);
                }
            }
            return AvailableStatus.TRUE;
        }

        // Tekil dosya ID'si ise Pixeldrain resmi info API'sinden detay al (ayna domain desteği ile)
        String[] domains = new String[] { "pixeldrain.com", "pixeldra.in", "pixeldrain.net" };
        for (String domain : domains) {
            try {
                Browser br = createNewBrowserInstance();
                br.setFollowRedirects(true);
                br.setConnectTimeout(6000);
                br.setReadTimeout(8000);
                br.getPage("https://" + domain + "/api/file/" + fid + "/info");
                URLConnectionAdapter con = br.getHttpConnection();
                if (con != null && con.getResponseCode() == 200) {
                    Map<String, Object> map = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                    Boolean success = (Boolean) map.get("success");
                    if (Boolean.FALSE.equals(success)) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    String name = (String) map.get("name");
                    Number size = (Number) map.get("size");
                    String sha256 = (String) map.get("hash_sha256");

                    if (name != null) link.setFinalFileName(name);
                    if (size != null) link.setDownloadSize(size.longValue());
                    if (sha256 != null && !sha256.isEmpty()) link.setSha256Hash(sha256);
                    return AvailableStatus.TRUE;
                } else if (con != null && con.getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } catch (PluginException pe) {
                throw pe;
            } catch (Exception e) {
                logger.log(e);
            }
        }

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        String downloadUrl = resolveDirectCdnUrl(link, getFID(link));
        logger.info("Starting Pixeldrain bypass download via: " + downloadUrl);

        this.br.setFollowRedirects(true);
        this.br.setConnectTimeout(10000);
        this.br.setReadTimeout(15000);
        dl = BrowserAdapter.openDownload(this.br, link, downloadUrl, true, 0);

        if (!looksLikeDownloadableContent(dl.getConnection())) {
            link.removeProperty("direct_cdn_url");
            this.br.followConnection(true);
            int code = this.br.getHttpConnection() != null ? this.br.getHttpConnection().getResponseCode() : 0;
            if (code == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String err = isTurkish()
                ? "Sunucu dosyayı vermedi (HTTP " + code + ")"
                : "Server did not return the file (HTTP " + code + ")";
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, err, 60 * 1000L);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.removeProperty("direct_cdn_url");
    }
}
