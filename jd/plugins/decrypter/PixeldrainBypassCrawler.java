package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import org.appwork.storage.TypeRef;
import org.appwork.utils.encoding.URLEncode;

@DecrypterPlugin(revision = "$Revision: 12 $", interfaceVersion = 3, names = { "PixeldrainBypass" }, urls = {
    "https?://(?:(?:www\\.)?(?:pixeldrain\\.(?:com|net|org|dev|nl|biz|tech)|pixeldra\\.in)/(?:(?:u|file|api/file|l|d)/[A-Za-z0-9_\\-]+(?:/.*)?|api/filesystem/[A-Za-z0-9_\\-]+(?:/.*)?|(?=[A-Za-z0-9]*\\d)(?=[A-Za-z0-9]*[A-Za-z])[A-Za-z0-9]{8,})|cdn[0-9]*\\.pixeldrain\\.eu\\.cc(?:/(?:d|ds|zip|dzip|api/file|api/filesystem))?/[A-Za-z0-9_\\-]+(?:/.*)?)"
})
@SuppressWarnings("unchecked")
public class PixeldrainBypassCrawler extends PluginForDecrypt {

    public PixeldrainBypassCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    private boolean shouldIgnoreFile(String name) {
        if (name == null || name.trim().isEmpty()) {
            return true;
        }
        return name.equals(".search_index.gz") || name.endsWith(".search_index.gz");
    }

    private DownloadLink createDirDownloadLink(String dirId, Map<String, Object> fileMap, FilePackage fp) {
        if (fileMap == null) return null;
        String name = (String) fileMap.get("name");
        Number size = (Number) fileMap.get("size");
        String encoded = name != null ? URLEncode.encodeURIComponent(name).replace("%2F", "/") : "";
        DownloadLink link = createDownloadlink("pixeldrainbypass://d/" + dirId + "/" + encoded);
        if (name != null) link.setFinalFileName(name);
        if (size != null) link.setDownloadSize(size.longValue());
        link.setAvailable(true);
        if (fp != null) link._setFilePackage(fp);
        return link;
    }

    private Map<String, Object> parseInitialNode(String html) {
        if (html == null) return null;
        int idx = html.indexOf("window.initial_node =");
        if (idx == -1) {
            idx = html.indexOf("initial_node =");
        }
        if (idx != -1) {
            int braceIdx = html.indexOf('{', idx);
            if (braceIdx != -1) {
                int depth = 0;
                int end = -1;
                boolean inString = false;
                boolean escape = false;
                for (int i = braceIdx; i < html.length(); i++) {
                    char c = html.charAt(i);
                    if (escape) {
                        escape = false;
                        continue;
                    }
                    if (c == '\\') {
                        escape = true;
                        continue;
                    }
                    if (c == '"') {
                        inString = !inString;
                        continue;
                    }
                    if (!inString) {
                        if (c == '{') {
                            depth++;
                        } else if (c == '}') {
                            depth--;
                            if (depth == 0) {
                                end = i;
                                break;
                            }
                        }
                    }
                }
                if (end != -1) {
                    String json = html.substring(braceIdx, end + 1);
                    try {
                        return restoreFromString(json, TypeRef.MAP);
                    } catch (Exception e) {
                        logger.log(e);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = param.getCryptedUrl();

        Browser br = createNewBrowserInstance();
        br.setFollowRedirects(true);
        br.setConnectTimeout(6000);
        br.setReadTimeout(8000);

        // 1. Doğrudan cdn.pixeldrain.eu.cc bypass linki girildiyse (Userscript veya harici araçlardan kopyalananlar)
        Regex cdnRegex = new Regex(url, "https?://cdn[0-9]*\\.pixeldrain\\.eu\\.cc/(.+)");
        if (cdnRegex.patternFind()) {
            String path = cdnRegex.getMatch(0);
            int qIdx = path.indexOf('?');
            if (qIdx != -1) path = path.substring(0, qIdx);
            int hIdx = path.indexOf('#');
            if (hIdx != -1) path = path.substring(0, hIdx);

            if (path.startsWith("api/file/")) {
                path = path.substring("api/file/".length());
            } else if (path.startsWith("api/filesystem/")) {
                path = "d/" + path.substring("api/filesystem/".length());
            }
            if (path.startsWith("d/") || path.startsWith("ds/") || path.startsWith("zip/") || path.startsWith("dzip/")) {
                DownloadLink link = createDownloadlink("pixeldrainbypass://" + path);
                link.setAvailable(true);
                decryptedLinks.add(link);
                return decryptedLinks;
            } else {
                DownloadLink link = processSingleFile(br, path);
                if (link != null) {
                    decryptedLinks.add(link);
                }
                return decryptedLinks;
            }
        }

        // 2. Tekil dosya linkleri (/u/ veya /file/ veya /api/file/)
        Regex fileRegex = new Regex(url, "/(?:u|file|api/file)/([A-Za-z0-9_\\-]+)");
        if (fileRegex.patternFind()) {
            String fid = fileRegex.getMatch(0);
            DownloadLink link = processSingleFile(br, fid);
            if (link != null) {
                decryptedLinks.add(link);
            }
            return decryptedLinks;
        }

        // 3. Galeri / Albüm linkleri (/l/)
        Regex galleryRegex = new Regex(url, "/l/([A-Za-z0-9_\\-]+)");
        if (galleryRegex.patternFind()) {
            String listId = galleryRegex.getMatch(0);
            try {
                String listApiUrl = "https://pixeldrain.com/api/list/" + listId;
                br.getPage(listApiUrl);
                URLConnectionAdapter con = br.getHttpConnection();
                if (con != null && con.getResponseCode() == 200) {
                    Map<String, Object> response = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                    String title = (String) response.get("title");
                    List<Map<String, Object>> files = (List<Map<String, Object>>) response.get("files");
                    FilePackage fp = null;
                    if (title != null && !title.trim().isEmpty()) {
                        fp = FilePackage.getInstance();
                        fp.setName(title.trim());
                    }
                    if (files != null) {
                        for (Map<String, Object> fileEntry : files) {
                            String id = (String) fileEntry.get("id");
                            if (id == null) continue;
                            String name = (String) fileEntry.get("name");
                            Number size = (Number) fileEntry.get("size");
                            String sha256 = (String) fileEntry.get("hash_sha256");

                            DownloadLink link = createDownloadlink("pixeldrainbypass://" + id);
                            if (name != null) link.setFinalFileName(name);
                            if (size != null) link.setDownloadSize(size.longValue());
                            if (sha256 != null && !sha256.isEmpty()) link.setSha256Hash(sha256);
                            link.setAvailable(true);
                            if (fp != null) link._setFilePackage(fp);
                            decryptedLinks.add(link);
                        }
                    }
                }
            } catch (Exception e) {
                logger.log(e);
            }
            return decryptedLinks;
        }

        // 4. Dizin / Klasör (/d/)
        Regex dirRegex = new Regex(url, "/d/([A-Za-z0-9_\\-]+)");
        if (dirRegex.patternFind()) {
            addDirectoryLinks(br, dirRegex.getMatch(0), decryptedLinks);
            return decryptedLinks;
        }

        // 5. Dosya sistemi API'si (/api/filesystem/)
        Regex fsRegex = new Regex(url, "/api/filesystem/([^?#]+)");
        if (fsRegex.patternFind()) {
            String path = fsRegex.getMatch(0);
            int slash = path.indexOf('/');
            if (slash > 0 && slash < path.length() - 1) {
                String dirId = path.substring(0, slash);
                String rawName = path.substring(slash + 1);
                String name = rawName;
                try {
                    name = java.net.URLDecoder.decode(rawName, "UTF-8");
                } catch (Exception ignored) {
                }
                String encoded = URLEncode.encodeURIComponent(name).replace("%2F", "/");
                DownloadLink link = createDownloadlink("pixeldrainbypass://d/" + dirId + "/" + encoded);
                link.setFinalFileName(name);
                link.setAvailable(true);
                decryptedLinks.add(link);
                return decryptedLinks;
            }
            addDirectoryLinks(br, path, decryptedLinks);
            return decryptedLinks;
        }

        // 6. Kısa kök dosya adresi (https://pixeldrain.com/Ab12Cd34). Sayfa adları rakamsız kaldığı için elenir.
        Regex rootRegex = new Regex(url, "https?://(?:www\\.)?(?:pixeldrain\\.(?:com|net|org|dev|nl|biz|tech)|pixeldra\\.in)/([A-Za-z0-9]{8,})(?:[?#].*)?$");
        if (rootRegex.patternFind()) {
            String fid = rootRegex.getMatch(0);
            if (fid.matches(".*\\d.*") && fid.matches(".*[A-Za-z].*")) {
                DownloadLink link = processSingleFile(br, fid);
                if (link != null && link.getFinalFileName() != null && link.isAvailable()) {
                    decryptedLinks.add(link);
                }
            }
        }

        return decryptedLinks;
    }

    private void addDirectoryLinks(Browser br, String dirId, ArrayList<DownloadLink> decryptedLinks) {
        try {
            br.getPage("https://pixeldrain.com/d/" + dirId);
            String html = br.getRequest().getHtmlCode();
            Map<String, Object> init = parseInitialNode(html);
            if (init == null) {
                br.getPage("https://pixeldrain.com/api/filesystem/" + dirId + "?stat");
                URLConnectionAdapter con = br.getHttpConnection();
                if (con != null && con.getResponseCode() == 200) {
                    init = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                }
            }
            if (init == null) {
                return;
            }

            String dirName = dirId;
            List<Map<String, Object>> pathList = (List<Map<String, Object>>) init.get("path");
            if (pathList != null && !pathList.isEmpty() && pathList.get(0).get("name") != null) {
                dirName = pathList.get(0).get("name").toString();
            }

            List<Map<String, Object>> children = (List<Map<String, Object>>) init.get("children");
            List<Map<String, Object>> validFiles = new ArrayList<Map<String, Object>>();
            if (children != null) {
                for (Map<String, Object> c : children) {
                    if (c != null && "file".equalsIgnoreCase((String) c.get("type"))) {
                        String name = (String) c.get("name");
                        if (!shouldIgnoreFile(name)) {
                            validFiles.add(c);
                        }
                    }
                }
            }

            if (validFiles.size() == 1 || (validFiles.isEmpty() && pathList != null && !pathList.isEmpty() && "file".equalsIgnoreCase((String) pathList.get(0).get("type")))) {
                Map<String, Object> single = !validFiles.isEmpty() ? validFiles.get(0) : pathList.get(0);
                DownloadLink link = createDownloadlink("pixeldrainbypass://ds/" + dirId);
                if (single != null) {
                    String name = (String) single.get("name");
                    Number size = (Number) single.get("size");
                    if (name != null) link.setFinalFileName(name);
                    if (size != null) link.setDownloadSize(size.longValue());
                }
                link.setAvailable(true);
                decryptedLinks.add(link);
                return;
            }

            if (!validFiles.isEmpty()) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(dirName);
                for (Map<String, Object> f : validFiles) {
                    DownloadLink link = createDirDownloadLink(dirId, f, fp);
                    if (link != null) {
                        decryptedLinks.add(link);
                    }
                }
            }
        } catch (Exception e) {
            logger.log(e);
        }
    }

    private DownloadLink processSingleFile(Browser br, String fid) {
        try {
            String infoUrl = "https://pixeldrain.com/api/file/" + fid + "/info";
            br.getPage(infoUrl);
            URLConnectionAdapter con = br.getHttpConnection();
            if (con != null && con.getResponseCode() == 200) {
                Map<String, Object> response = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                Boolean success = (Boolean) response.get("success");
                if (Boolean.FALSE.equals(success)) {
                    DownloadLink link = createDownloadlink("pixeldrainbypass://" + fid);
                    link.setAvailable(false);
                    return link;
                }
                String name = (String) response.get("name");
                Number size = (Number) response.get("size");
                String sha256 = (String) response.get("hash_sha256");

                DownloadLink link = createDownloadlink("pixeldrainbypass://" + fid);
                if (name != null) link.setFinalFileName(name);
                if (size != null) link.setDownloadSize(size.longValue());
                if (sha256 != null && !sha256.isEmpty()) link.setSha256Hash(sha256);
                link.setAvailable(true);
                return link;
            }
        } catch (Exception e) {
            logger.log(e);
        }

        DownloadLink fallback = createDownloadlink("pixeldrainbypass://" + fid);
        fallback.setAvailable(true);
        return fallback;
    }
}
