import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { NotificationItem } from '../core/models';
import { SessionStore } from '../core/session.store';
import { StoreApi, errorMessage } from '../core/store.api';

@Component({
  imports: [DatePipe],
  selector: 'app-notificaciones',
  templateUrl: './notificaciones.html',
})
export class Notificaciones implements OnInit {
  private readonly api = inject(StoreApi);
  private readonly session = inject(SessionStore);

  protected readonly items = signal<NotificationItem[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly unread = computed(() => this.items().filter((item) => !item.read).length);

  ngOnInit(): void {
    this.reload();
  }

  protected markRead(item: NotificationItem): void {
    this.api.markRead(item.id).subscribe({
      next: (updated) => {
        this.items.update((list) => list.map((entry) => (entry.id === updated.id ? updated : entry)));
        this.session.refreshBadges();
      },
      error: (err) => this.error.set(errorMessage(err, 'No se pudo marcar como leída')),
    });
  }

  protected markAll(): void {
    this.api.markAllRead().subscribe({
      next: () => {
        this.items.update((list) => list.map((entry) => ({ ...entry, read: true })));
        this.session.refreshBadges();
      },
      error: (err) => this.error.set(errorMessage(err, 'No se pudieron marcar las notificaciones')),
    });
  }

  protected icon(type: string): string {
    switch (type) {
      case 'ORDER':
        return '🧾';
      case 'SHIPPING':
        return '🚚';
      default:
        return 'ℹ️';
    }
  }

  private reload(): void {
    this.loading.set(true);
    this.api.notifications().subscribe({
      next: (items) => {
        this.items.set(items);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(errorMessage(err, 'No se pudieron cargar las notificaciones'));
      },
    });
  }
}
