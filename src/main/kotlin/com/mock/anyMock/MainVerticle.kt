package com.mock.anyMock

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.parser.OpenAPIV3Parser
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router

class MainVerticle : AbstractVerticle() {
  private fun getVertxHttpMethod(m: PathItem.HttpMethod): HttpMethod = HttpMethod.valueOf(m.name)

  override fun start(startPromise: Promise<Void>) {
    val server = vertx.createHttpServer()
    val router = Router.router(vertx)
    server
      .requestHandler(router)
      .listen(8888) { http ->
        if (http.succeeded()) {
          startPromise.complete()
          println("HTTP server started on port 8888")
        } else {
          startPromise.fail(http.cause());
        }
      }
    router.route(HttpMethod.GET, "/routings").handler { ctx -> ctx.response().end("Hello there") }
    router.route(HttpMethod.GET, "/routings/:name").handler { ctx -> ctx.response().end("Hello ${ctx.pathParam("name")}") }


    val openAPI: OpenAPI = OpenAPIV3Parser().read("docs/sample-openapi.yaml")
//    println(openAPI)
    openAPI.paths.forEach { (path, item) ->
      item.readOperationsMap().forEach { (m, o) ->
        val paramsPath = o.parameters
          ?.filter { it.getIn() == "path" }
          ?.map { path.replace("{${it.name}}", ":${it.name}") }
          ?.lastOrNull() ?: path
        val opStr = "$m $paramsPath"
        router.route(getVertxHttpMethod(m), paramsPath).handler { ctx ->
          ctx.response().end(o.responses["200"]?.content?.get("application/json")?.example?.toString() ?: opStr)
        }
        println("Created $opStr")
      }
    }
  }
}

