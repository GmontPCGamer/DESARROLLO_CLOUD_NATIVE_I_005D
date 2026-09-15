import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminStockRow } from '../core/models';
import { SessionStore } from '../core/session.store';
import { StoreApi, errorMessage } from '../core/store.api';
import { formatClp } from '../core/money';

/**
 * Panel de administración. Requiere ROLE_ADMIN: el BFF responde 403 si el
 * token no trae el rol (claim `roles`) ni el usuario está en APP_ADMIN_USERS.
 */
@Component({
  imports: [RouterLink],
  selector: 'app-admin',
  templateUrl: './admin.html',
})
export class Admin implements OnInit {
  private readonly api = inject(StoreApi);
  protected readonly session = inject(SessionStore);

  protected readonly rows = signal<AdminStockRow[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly forbidden = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly busy = signal<number | null>(null);
  protected readonly quantities = signal<Record<number, number>>({});
  protected readonly formatClp = formatClp;

  ngOnInit(): void {
    this.load();
  }

  protected qty(productId: number): number {
    return this.quantities()[productId] ?? 5;
  }

  protected setQty(productId: number, value: string): void {
    const parsed = Math.max(1, Math.min(999, Number(value) || 1));
    this.quantities.update((current) => ({ ...current, [productId]: parsed }));
  }

  protected restock(row: AdminStockRow): void {
    this.busy.set(row.product.id);
    this.error.set(null);
    this.api.restock(row.product.id, this.qty(row.product.id)).subscribe({
      next: (updated) => {
        this.rows.update((list) => list.map((item) => (item.product.id === updated.product.id ? updated : item)));
        this.busy.set(null);
        this.message.set(`Repuesto ${row.product.name}: ahora ${updated.product.stock} en catálogo / ${updated.reservable} reservables`);
        setTimeout(() => this.message.set(null), 4000);
      },
      error: (err) => {
        this.busy.set(null);
        this.error.set(errorMessage(err, 'No se pudo reponer stock'));
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.api.adminInventory().subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        if (err?.status === 403) {
          this.forbidden.set(true);
        } else {
          this.error.set(errorMessage(err, 'No se pudo cargar el inventario'));
        }
      },
    });
  }
}
