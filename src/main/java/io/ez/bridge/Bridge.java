package io.ez.bridge;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

public class Bridge extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        super.start();
        WebClient client = WebClient.create(vertx);
        EventBus eventBus = vertx.eventBus();

        eventBus.consumer("modules.save", (o) -> {
            JsonObject module = (JsonObject) o.body();
            client.post(80, module.getString("url"), "/save")
                    .sendBuffer(module.toBuffer(), ar -> {
                        if (ar.succeeded()) {
                            System.out.println("ok");
                        }
                    });
        });


    }
}
