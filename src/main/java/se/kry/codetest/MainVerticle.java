package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  private HashMap<String, String> servicesMap = new HashMap<>();
  //TODO use this
  private DBConnector connector;
  private BackgroundPoller poller = new BackgroundPoller();

  @Override
  public void start(Future<Void> startFuture) {
    connector = new DBConnector(vertx);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    servicesMap.put("https://www.kry.se", "UNKNOWN");
    loadServiceFromDB(servicesMap);

    vertx.setPeriodic(1000 * 30, timerId -> {
      poller.pollServices(servicesMap);
    });

    setRoutes(router);
    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(8080, result -> {
          if (result.succeeded()) {
            System.out.println("KRY code test service started");
            startFuture.complete();
          } else {
            startFuture.fail(result.cause());
          }
        });
  }

  private void setRoutes(Router router){
    // note *
    router.route("/*").handler(StaticHandler.create());

    router.get("/service").handler(req -> {

     List<JsonObject> jsonServices = servicesMap
          .entrySet()
          .stream()
          .map(service ->
              new JsonObject()
                  .put("name", service.getKey())
                  .put("status", service.getValue()))
          .collect(Collectors.toList());
      req.response()
          .putHeader("content-type", "application/json")
          .end(new JsonArray(jsonServices).encode());
    });

    router.post("/service").handler(req -> {
      JsonObject jsonBody = req.getBodyAsJson();
      String action = jsonBody.getString("action");
      String url = jsonBody.getString("url");
      String name = jsonBody.getString("name");
      Date date = Calendar.getInstance().getTime();
      DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
      String strDate = dateFormat.format(date);
      if (action.equals("add"))
      {
        insertServiceToDB(url, name, strDate);
        servicesMap.put(jsonBody.getString("url"), "UNKNOWN");
      } else
      {
        deleteServiceFromDB(name);
//        servicesMap.remove(jsonBody.getString("url"), "UNKNOWN");
      }
      req.response()
          .putHeader("content-type", "text/plain")
          .end("OK");
    });
  }

  private void loadServiceFromDB(HashMap<String, String> servicesMap){
    connector.query("SELECT * FROM serviceDB").setHandler(res -> {
      if (res.succeeded()) {
//        System.out.println(res.result().getResults());
        System.out.println("Service successfully retrieved from DB");
        List<JsonArray> service = res.result().getResults();
        int  i = 0;
        for (JsonArray s: service){
          servicesMap.put(s.getString(0), "UNKNOWN");
          System.out.println("ServiceDB entry[" + i++ + "]: " + s);
        }
      } else {
        System.out.println("Failed to load data from ServiceDB or DB does not exist.");
      }
    });
  }

  private void insertServiceToDB(String url, String name, String timeStamp) {
    JsonArray param = new JsonArray();
    param.add(url).add(name).add(timeStamp);
    connector.query("INSERT INTO serviceDB (url, name, timeStamp) VALUES (?,?,?)", param).setHandler(res -> {
      if (res.succeeded()) {
        System.out.println("Service successfully inserted into DB");
      } else {
        System.out.println("Failed to insert service to DB");
      }
    });
  }

  private void deleteServiceFromDB(String name) {
    JsonArray param = new JsonArray();
    param.add(name);
    connector.query("DELETE FROM serviceDB WHERE name=?", param).setHandler(res -> {
      if (res.succeeded()) {
        System.out.println("Service successfully deleted from DB");
      } else {
        System.out.println("Failed to delete service from DB");
      }
    });
  }

}



