package io.ez.bridge;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.Router;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class Bridge extends AbstractVerticle {

    private JsonArray modules = new JsonArray();

    private HttpServer httpServer;
    private WebClient  client;
    private Router     router;
    private EventBus   eventBus;

    @Override
    public void start() throws Exception {
        super.start();
        this.client = WebClient.create(vertx);
        this.httpServer = this.vertx.createHttpServer();
        this.router = Router.router(vertx);

        this.eventBus = vertx.eventBus();

        eventBus.consumer("module.save", (o) -> {
            JsonObject module = (JsonObject) o.body();

            client.post(8777, module.getString("url"), "/config")
                    .sendBuffer(module.toBuffer(), ar -> {
                        if (ar.succeeded()) {
                            System.out.println("ok");
                        }
                    });
        });

        for (Object m : this.modules) {
            JsonObject module = (JsonObject)m;
            eventBus.consumer("modules."+module.getInteger("id")+".change", (o) -> {
                JsonObject innerModule = (JsonObject) o.body();
                client.post(80, innerModule.getString("url"), "/change")
                        .sendBuffer(innerModule.toBuffer(), ar -> {
                            if (ar.succeeded()) {
                                System.out.println("ok");
                            }
                        });
            });
        }

        // TODO Remove after testing.
        eventBus.consumer("module.new", (o) -> {
            JsonObject module = (JsonObject) o.body();

            module.put("devices", new JsonArray().add(new JsonObject().put("id", 1).put("type", "rgbled")));

            eventBus.publish("module.save", module);
        });

        router.route().handler(BodyHandler.create());

        // Routes
        router.route("/").handler(this::getRoot);
        router.post("/hello").handler(this::getHello);

        this.httpServer.requestHandler(router::accept).listen(8777);

        System.out.println("Server started");
    }

    /**
     * GET /
     * @param routingContext
     */
    private void getRoot(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        response
                .putHeader("content-type", "text/html")
                .end("<h1>The Bridge</h1>");
    }

    /**
     * POST /hello
     * @param routingContext
     */
    private void getHello(RoutingContext routingContext) {
        String remoteAddress = routingContext.request().connection().remoteAddress().toString();

        System.out.println("Received hello from " + remoteAddress);

        JsonObject jsonBody = routingContext.getBodyAsJson();
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "application/json");

        if (jsonBody == null) {
            response.setStatusCode(400);
            response.end("{\"error\":\"Invalid JSON or no body present.\"}");
            return;
        }

        this.eventBus.publish("module.new", jsonBody.put("url", remoteAddress));

        response.setStatusCode(200);
        response.end("{\"status\":\"ok\"}");
    }
}
