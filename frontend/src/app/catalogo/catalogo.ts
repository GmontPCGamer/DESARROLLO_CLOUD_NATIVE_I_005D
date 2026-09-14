import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CATEGORIES, Product } from '../core/models';
import { StoreApi } from '../core/store.api';
import { formatClp } from '../core/money';
import { AuthService } from '../auth/auth.service';

@Component({
  imports: [RouterLink],
  selector: 'app-catalogo',
  templateUrl: './catalogo.html',
})
export class Catalogo implements OnInit {
  private readonly api = inject(StoreApi);
  private readonly auth = inject(AuthService);

  protected readonly categories = CATEGORIES;
  protected readonly products = signal<Product[]>([]);
  protected readonly selected = signal('');
  protected readonly query = signal('');
  protected readonly visibleProducts = computed(() => {
    const term = this.query().trim().toLowerCase();
    if (!term) {
      return this.products();
    }
    return this.products().filter((product) =>
      [product.name, product.brand, product.category].some((value) => value.toLowerCase().includes(term)),
    );
  });
  protected readonly error = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly formatClp = formatClp;

  ngOnInit(): void {
    this.load();
  }

  protected filter(category: string): void {
    this.selected.set(category);
    this.load();
  }

  protected search(value: string): void {
    this.query.set(value);
  }

  protected add(product: Product): void {
    if (!this.auth.activeAccount) {
      this.auth.login();
      return;
    }
    this.api.addToCart(product.id, 1).subscribe({
      next: () => this.message.set(product.name + ' se agregó al carrito'),
      error: () => this.error.set('No se pudo agregar al carrito'),
    });
  }

  private load(): void {
    this.api.products(this.selected() || undefined).subscribe({
      next: (items) => {
        this.products.set(items);
        this.error.set(null);
      },
      error: () => this.error.set('No se pudo cargar el catálogo'),
    });
  }
}
