package se.kry.codetest;

import io.vertx.core.Future;

import java.awt.datatransfer.SystemFlavorMap;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.net.HttpURLConnection.*;

public class BackgroundPoller {

  public Future<List<String>> pollServices(Map<String, String> services) {
    //TODO
    services.forEach((key, value) -> services.put(key, pollService(key)));
    return Future.failedFuture("TODO");
  }

  private String pollService(String key) {

    String status;
    try {
      URL url = new URL (key);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setConnectTimeout(1000);
      conn.connect();

      switch (conn.getResponseCode()) {
        case HTTP_OK:
          status = "** HTTP OK **";
          break;
        case HTTP_BAD_GATEWAY:
          status = "** HTTP_BAD_GATEWAY **";
          break;
        case HTTP_GATEWAY_TIMEOUT:
          status = "** HTTP_GATEWAY_TIMEOUT **";
          break;
        case HTTP_NOT_FOUND:
          status = "** HTTP_NOT_FOUND **";
          break;
        default:
          status = "** Connection Error **";
      }
    } catch (IOException e) {
      status = "** IOException Error **";
    }
    return status;
  }
}
