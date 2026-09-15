import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Order, PAYMENT_METHODS } from '../core/models';
import { StoreApi, errorMessage } from '../core/store.api';
import { formatClp } from '../core/money';

@Component({
  imports: [RouterLink, DatePipe],
  selector: 'app-compras',
  templateUrl: './compras.html',
})
export class Compras implements OnInit {
  private readonly api = inject(StoreApi);
  private readonly route = inject(ActivatedRoute);

  protected readonly orders = signal<Order[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly highlighted = signal<number | null>(null);
  protected readonly formatClp = formatClp;

  ngOnInit(): void {
    const nueva = Number(this.route.snapshot.queryParamMap.get('nueva'));
    if (nueva) {
      this.highlighted.set(nueva);
    }
    this.api.orders().subscribe({
      next: (orders) => {
        this.orders.set(orders);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(errorMessage(err, 'No se pudieron cargar las compras'));
      },
    });
  }

  protected paymentLabel(method: string | null): string {
    return PAYMENT_METHODS.find((item) => item.id === method)?.label ?? method ?? 'Simulado';
  }

  protected shortId(value: string | null): string {
    return value ? value.slice(0, 8).toUpperCase() : '—';
  }
}
