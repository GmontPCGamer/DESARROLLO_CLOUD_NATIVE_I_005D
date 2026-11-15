import { Component, OnInit, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { JsonPipe } from '@angular/common';
import { environment } from '../../environments/environment';
import { errorMessage } from '../core/store.api';
import { formatClp } from '../core/money';

interface Topic { name: string; partitions: number; replicas: number; protectedTopic: boolean; }
interface Sales { orderCount: number; revenue: number; units: number; currency: string; }
interface Group { groupId: string; state: string; }
@Component({
  selector: 'app-streams-admin',
  imports: [JsonPipe],
  templateUrl: './streams-admin.html',
})
export class StreamsAdmin implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly base = environment.backendApiUrl + '/admin';
  protected readonly topics = signal<Topic[]>([]);
  protected readonly groups = signal<Group[]>([]);
  protected readonly sales = signal<Sales | null>(null);
  protected readonly details = signal<unknown>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly topicName = signal('demo.actividad');
  protected readonly retentionHours = signal(168);
  protected readonly formatClp = formatClp;
  protected readonly kafkaUi = 'http://127.0.0.1:18093/dev/kafka/';
  protected readonly rabbitUi = 'http://127.0.0.1:18072/';
  ngOnInit(): void { this.refresh(); }
  protected refresh(): void {
    this.error.set(null);
    this.http.get<Topic[]>(this.base+'/kafka/topics').subscribe({ next:v=>this.topics.set(v), error:e=>this.fail(e) });
    this.http.get<Group[]>(this.base+'/kafka/groups').subscribe({ next:v=>this.groups.set(v), error:e=>this.fail(e) });
    this.http.get<Sales>(this.base+'/analytics/sales').subscribe({ next:v=>this.sales.set(v), error:e=>this.fail(e) });
  }
  protected create(): void {
    const name=this.topicName().trim();
    if (!name.startsWith('demo.')) { this.error.set('Usa el prefijo demo. para los topics de prueba.'); return; }
    this.mutate(this.http.post(this.base+'/kafka/topics',{
      name, partitions:3, replicas:2, config:{'cleanup.policy':'delete','retention.ms':String(this.retentionHours()*3600000)}
    }), 'Topic creado');
  }
  protected inspect(topic: Topic): void {
    this.http.get(this.base+'/kafka/topics/'+encodeURIComponent(topic.name)+'/config')
      .subscribe({next:v=>this.details.set({topic:topic.name,config:v}),error:e=>this.fail(e)});
  }
  protected update(topic: Topic): void {
    this.mutate(this.http.patch(this.base+'/kafka/topics/'+encodeURIComponent(topic.name),{
      config:{'retention.ms':String(this.retentionHours()*3600000)}
    }), 'Retención actualizada');
  }
  protected addPartition(topic: Topic): void {
    this.mutate(this.http.patch(this.base+'/kafka/topics/'+encodeURIComponent(topic.name),{
      partitions:topic.partitions+1
    }), 'Partición agregada');
  }
  protected remove(topic: Topic): void {
    this.mutate(this.http.delete(this.base+'/kafka/topics/'+encodeURIComponent(topic.name)), 'Topic de prueba eliminado');
  }
  protected inspectGroup(group: Group): void {
    this.http.get(this.base+'/kafka/groups/'+encodeURIComponent(group.groupId))
      .subscribe({next:v=>this.details.set(v),error:e=>this.fail(e)});
  }
  private mutate(request: import('rxjs').Observable<unknown>, message:string):void {
    this.busy.set(true);this.error.set(null);
    request.subscribe({ next:()=>{this.busy.set(false);this.message.set(message);this.refresh();},
      error:e=>{this.busy.set(false);this.fail(e);} });
  }
  private fail(error:unknown):void { this.error.set(errorMessage(error,'No se pudo consultar la plataforma de eventos')); }
}
