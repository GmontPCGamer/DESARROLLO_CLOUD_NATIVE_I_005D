package com.cloudnative.backend.controller;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
@RestController
@RequestMapping("/api/admin")
public class StreamingProxyController {
  private final RestClient kafka;
  private final RestClient analytics;
  public StreamingProxyController(@Value("${app.services.kafka-admin}")String kafka,
      @Value("${app.services.analytics}")String analytics){
    this.kafka=RestClient.builder().baseUrl(kafka).build();
    this.analytics=RestClient.builder().baseUrl(analytics).build();
  }
  @GetMapping("/kafka/topics") public ResponseEntity<byte[]> topics(@AuthenticationPrincipal Jwt jwt){return forward(kafka,HttpMethod.GET,"/api/kafka/topics",null,jwt);}
  @GetMapping("/kafka/topics/{name}/config") public ResponseEntity<byte[]> config(@PathVariable String name,@AuthenticationPrincipal Jwt jwt){return forward(kafka,HttpMethod.GET,"/api/kafka/topics/"+name+"/config",null,jwt);}
  @PostMapping("/kafka/topics") public ResponseEntity<byte[]> create(@RequestBody byte[] body,@AuthenticationPrincipal Jwt jwt){return forward(kafka,HttpMethod.POST,"/api/kafka/topics",body,jwt);}
  @PatchMapping("/kafka/topics/{name}") public ResponseEntity<byte[]> update(@PathVariable String name,@RequestBody byte[] body,@AuthenticationPrincipal Jwt jwt){return forward(kafka,HttpMethod.PATCH,"/api/kafka/topics/"+name,body,jwt);}
  @DeleteMapping("/kafka/topics/{name}") public ResponseEntity<byte[]> delete(@PathVariable String name,@AuthenticationPrincipal Jwt jwt){return forward(kafka,HttpMethod.DELETE,"/api/kafka/topics/"+name,null,jwt);}
  @GetMapping("/kafka/groups") public ResponseEntity<byte[]> groups(@AuthenticationPrincipal Jwt jwt){return forward(kafka,HttpMethod.GET,"/api/kafka/groups",null,jwt);}
  @GetMapping("/kafka/groups/{name}") public ResponseEntity<byte[]> group(@PathVariable String name,@AuthenticationPrincipal Jwt jwt){return forward(kafka,HttpMethod.GET,"/api/kafka/groups/"+name,null,jwt);}
  @GetMapping("/analytics/sales") public ResponseEntity<byte[]> sales(@AuthenticationPrincipal Jwt jwt){return forward(analytics,HttpMethod.GET,"/api/analytics/sales",null,jwt);}
  @GetMapping("/analytics/products") public ResponseEntity<byte[]> products(@AuthenticationPrincipal Jwt jwt){return forward(analytics,HttpMethod.GET,"/api/analytics/products",null,jwt);}
  private ResponseEntity<byte[]> forward(RestClient client,HttpMethod method,String path,byte[] body,Jwt jwt){
    var request=client.method(method).uri(path).header(HttpHeaders.AUTHORIZATION,"Bearer "+jwt.getTokenValue());
    if(body!=null)return request.contentType(MediaType.APPLICATION_JSON).body(body).exchange(this::response);
    return request.exchange(this::response);
  }
  private ResponseEntity<byte[]> response(org.springframework.http.HttpRequest request,RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response)throws IOException{
    return ResponseEntity.status(response.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(response.getBody().readAllBytes());
  }
}
