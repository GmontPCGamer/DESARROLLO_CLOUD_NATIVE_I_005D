import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Cart } from '../core/models';
import { StoreApi } from '../core/store.api';
import { formatClp } from '../core/money';

@Component({
  imports: [RouterLink],
  selector: 'app-carrito',
  templateUrl: './carrito.html',
})
export class Carrito implements OnInit {
  private readonly api = inject(StoreApi);
  private readonly router = inject(Router);

  protected readonly cart = signal<Cart | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly formatClp = formatClp;
  protected readonly commune = signal('Santiago');
  protected readonly shippingQuote = signal<{ price: number; promise: string } | null>(null);
  protected readonly paymentStatus = signal<string | null>(null);

  ngOnInit(): void {
    this.reload();
  }

  protected changeQty(productId: number, quantity: number): void {
    this.api.updateQty(productId, quantity).subscribe({
      next: (cart) => this.cart.set(cart),
      error: () => this.error.set('No se pudo actualizar el carrito'),
    });
  }

  protected remove(productId: number): void {
    this.api.removeItem(productId).subscribe({
      next: (cart) => this.cart.set(cart),
      error: () => this.error.set('No se pudo quitar el producto'),
    });
  }

  protected checkout(): void {
    this.loading.set(true);
    const total = this.cart()?.total ?? 0;
    this.api.shipping('Metropolitana', this.commune()).subscribe({
      next: (shipping) => {
        this.shippingQuote.set(shipping);
        this.api.payment(total + shipping.price).subscribe({
          next: (payment) => {
            this.paymentStatus.set(payment.status);
            this.api.checkout().subscribe({
              next: () => this.router.navigateByUrl('/compras'),
              error: () => {
                this.loading.set(false);
                this.error.set('El pago fue autorizado, pero no se pudo confirmar la orden.');
              },
            });
          },
          error: () => {
            this.loading.set(false);
            this.error.set('No se pudo autorizar el pago.');
          },
        });
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No se pudo cotizar el despacho.');
      },
    });
  }

  private reload(): void {
    this.api.cart().subscribe({
      next: (cart) => this.cart.set(cart),
      error: () => this.error.set('No se pudo cargar el carrito'),
    });
  }
}
