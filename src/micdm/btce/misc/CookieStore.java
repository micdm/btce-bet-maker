package micdm.btce.misc;

import com.google.gson.Gson;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class CookieStore implements CookieJar {

    private final String FILE_PATH = "/tmp/btce_cookies.json";

    private final Gson gson;
    private final Logger logger;

    private List<Cookie> stored = new ArrayList<>();

    public CookieStore(Gson gson, Logger logger) {
        this.gson = gson;
        this.logger = logger;
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        logger.debug("Storing {} cookies from {}..,", cookies.size(), url);
        Map<String, Cookie> result = new HashMap<>();
        for (Cookie cookie: getStored()) {
            result.put(cookie.name(), cookie);
        }
        for (Cookie cookie: cookies) {
            result.put(cookie.name(), cookie);
        }
        store(result.values());
    }

    private List<Cookie> getStored() {
        return stored;
//        try {
//            return new ArrayList<>(Arrays.asList(gson.fromJson(new FileReader(FILE_PATH), Cookie[].class)));
//        } catch (FileNotFoundException e) {
//            return new ArrayList<>();
//        }
    }

    private void store(Collection<Cookie> cookies) {
        stored = new ArrayList<>(cookies);
//        try {
//            FileWriter writer = new FileWriter(FILE_PATH);
//            gson.toJson(cookies, writer);
//            writer.close();
//        } catch (IOException e) {
//            logger.error("Cannot write cookies to external file", e);
//        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        logger.debug("Loading cookies for {}...", url);
        return getStored().stream()
            .filter(cookie -> cookie.matches(url))
            .collect(Collectors.toList());
    }
}
