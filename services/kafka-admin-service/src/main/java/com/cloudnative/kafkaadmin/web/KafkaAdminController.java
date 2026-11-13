package com.cloudnative.kafkaadmin.web;
import com.cloudnative.kafkaadmin.service.TopicAdministration;
import com.cloudnative.kafkaadmin.service.TopicAdministration.*;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api")
public class KafkaAdminController {
  private final TopicAdministration topics;
  public KafkaAdminController(TopicAdministration topics){this.topics=topics;}
  @GetMapping("/public/health") public Map<String,String> health(){return Map.of("status","UP","service","kafka-admin-service");}
  @GetMapping("/kafka/topics") public List<TopicView> topics(){return topics.topics();}
  @GetMapping("/kafka/topics/{name}/config") public Map<String,String> config(@PathVariable String name){return topics.config(name);}
  @PostMapping("/kafka/topics") public Map<String,String> create(@RequestBody CreateTopic request){topics.create(request);return Map.of("status","created");}
  @PatchMapping("/kafka/topics/{name}") public Map<String,String> update(@PathVariable String name,@RequestBody UpdateTopic request){topics.update(name,request);return Map.of("status","updated");}
  @DeleteMapping("/kafka/topics/{name}") public Map<String,String> delete(@PathVariable String name){topics.delete(name);return Map.of("status","deleted");}
  @GetMapping("/kafka/groups") public List<GroupView> groups(){return topics.groups();}
  @GetMapping("/kafka/groups/{name}") public GroupDetails group(@PathVariable String name){return topics.group(name);}
}
