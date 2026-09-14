import { Component, OnInit, inject, signal } from '@angular/core';
import { NotificationItem } from '../core/models';
import { StoreApi } from '../core/store.api';

@Component({
  selector: 'app-notificaciones',
  templateUrl: './notificaciones.html',
})
export class Notificaciones implements OnInit {
  private readonly api = inject(StoreApi);

  protected readonly items = signal<NotificationItem[]>([]);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.reload();
  }

  protected markRead(item: NotificationItem): void {
    this.api.markRead(item.id).subscribe({
      next: () => this.reload(),
      error: () => this.error.set('No se pudo marcar como leída'),
    });
  }

  private reload(): void {
    this.api.notifications().subscribe({
      next: (items) => this.items.set(items),
      error: () => this.error.set('No se pudieron cargar las notificaciones'),
    });
  }
}
