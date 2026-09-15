import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Cart, PAYMENT_METHODS, REGIONS, ShippingQuote } from '../core/models';
import { StoreApi, errorMessage } from '../core/store.api';
import { formatClp } from '../core/money';

/**
 * Carrito y checkout. El pago es SIMULADO: el BFF cotiza el despacho, pide una
 * autorización ficticia al payment-service y confirma la orden en un solo paso.
 */
@Component({
  imports: [RouterLink],
  selector: 'app-carrito',
  templateUrl: './carrito.html',
})
export class Carrito implements OnInit {
  private readonly api = inject(StoreApi);
  private readonly router = inject(Router);

  protected readonly regions = REGIONS;
  protected readonly paymentMethods = PAYMENT_METHODS;
  protected readonly formatClp = formatClp;

  protected readonly cart = signal<Cart | null>(null);
  protected readonly loading = signal(true);
  protected readonly busyItem = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly region = signal<string>('Metropolitana');
  protected readonly commune = signal('Santiago');
  protected readonly paymentMethod = signal<string>('SIMULATED_CARD');

  protected readonly quote = signal<ShippingQuote | null>(null);
  protected readonly quoting = signal(false);
  protected readonly checkingOut = signal(false);
  protected readonly step = signal<string | null>(null);

  protected readonly shippingCost = computed(() => this.quote()?.price ?? 0);
  protected readonly total = computed(() => (this.cart()?.total ?? 0) + this.shippingCost());
  protected readonly freeShippingGap = computed(() => Math.max(0, 500000 - (this.cart()?.total ?? 0)));

  ngOnInit(): void {
    this.reload();
  }

  protected changeQty(productId: number, quantity: number): void {
    this.busyItem.set(productId);
    this.error.set(null);
    this.api.updateQty(productId, quantity).subscribe({
      next: (cart) => {
        this.cart.set(cart);
        this.busyItem.set(null);
        this.quote.set(null); // el subtotal cambió: la cotización debe rehacerse
      },
      error: (err) => {
        this.busyItem.set(null);
        this.error.set(errorMessage(err, 'No se pudo actualizar el carrito'));
      },
    });
  }

  protected remove(productId: number): void {
    this.busyItem.set(productId);
    this.api.removeItem(productId).subscribe({
      next: (cart) => {
        this.cart.set(cart);
        this.busyItem.set(null);
        this.quote.set(null);
      },
      error: (err) => {
        this.busyItem.set(null);
        this.error.set(errorMessage(err, 'No se pudo quitar el producto'));
      },
    });
  }

  protected clear(): void {
    this.api.clearCart().subscribe({
      next: (cart) => {
        this.cart.set(cart);
        this.quote.set(null);
      },
      error: (err) => this.error.set(errorMessage(err, 'No se pudo vaciar el carrito')),
    });
  }

  protected quoteShipping(): void {
    if (!this.commune().trim()) {
      this.error.set('Indica la comuna de despacho');
      return;
    }
    this.quoting.set(true);
    this.error.set(null);
    this.api.shipping(this.region(), this.commune().trim()).subscribe({
      next: (quote) => {
        this.quote.set(quote);
        this.quoting.set(false);
      },
      error: (err) => {
        this.quoting.set(false);
        this.error.set(errorMessage(err, 'No se pudo cotizar el despacho'));
      },
    });
  }

  protected checkout(): void {
    if (!this.commune().trim()) {
      this.error.set('Indica la comuna de despacho');
      return;
    }
    this.checkingOut.set(true);
    this.error.set(null);
    this.step.set('Cotizando despacho, autorizando pago simulado y reservando stock…');
    this.api
      .checkout({ region: this.region(), commune: this.commune().trim(), paymentMethod: this.paymentMethod() })
      .subscribe({
        next: (result) => {
          this.checkingOut.set(false);
          this.step.set(null);
          this.router.navigate(['/compras'], { queryParams: { nueva: result.order.id } });
        },
        error: (err) => {
          this.checkingOut.set(false);
          this.step.set(null);
          this.error.set(errorMessage(err, 'No se pudo confirmar la compra'));
          this.reload(); // el stock puede haber cambiado
        },
      });
  }

  private reload(): void {
    this.loading.set(true);
    this.api.cart().subscribe({
      next: (cart) => {
        this.cart.set(cart);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(errorMessage(err, 'No se pudo cargar el carrito'));
      },
    });
  }
}
