import { Component, OnInit, inject, signal } from '@angular/core';
import { Order } from '../core/models';
import { StoreApi } from '../core/store.api';
import { formatClp } from '../core/money';

@Component({
  selector: 'app-compras',
  templateUrl: './compras.html',
})
export class Compras implements OnInit {
  private readonly api = inject(StoreApi);

  protected readonly orders = signal<Order[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly formatClp = formatClp;

  ngOnInit(): void {
    this.api.orders().subscribe({
      next: (orders) => this.orders.set(orders),
      error: () => this.error.set('No se pudieron cargar las compras'),
    });
  }
}
