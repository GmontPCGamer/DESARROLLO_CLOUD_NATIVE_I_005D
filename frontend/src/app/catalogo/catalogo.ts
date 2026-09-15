import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CATEGORIES, Product } from '../core/models';
import { StoreApi, errorMessage } from '../core/store.api';
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
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly categories = CATEGORIES;
  protected readonly products = signal<Product[]>([]);
  protected readonly selected = signal('');
  protected readonly query = signal('');
  protected readonly loading = signal(true);
  protected readonly adding = signal<number | null>(null);
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
  protected readonly skeletons = Array.from({ length: 8 }, (_, index) => index);

  ngOnInit(): void {
    // La categoría viaja en la URL (?c=CELULARES) para poder enlazarla desde la home.
    this.route.queryParamMap.subscribe((params) => {
      const category = params.get('c') ?? '';
      const known = CATEGORIES.some((item) => item.id === category);
      this.selected.set(known ? category : '');
      this.load();
    });
  }

  protected filter(category: string): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { c: category || null },
      queryParamsHandling: 'merge',
    });
  }

  protected search(value: string): void {
    this.query.set(value);
  }

  protected stockClass(stock: number): string {
    if (stock < 1) return 'stock-out';
    if (stock <= 5) return 'stock-low';
    return 'stock-ok';
  }

  protected stockLabel(stock: number): string {
    if (stock < 1) return 'Agotado';
    if (stock <= 5) return `Últimas ${stock} unidades`;
    return `${stock} disponibles`;
  }

  protected add(product: Product): void {
    if (!this.auth.activeAccount) {
      this.auth.login();
      return;
    }
    this.error.set(null);
    this.adding.set(product.id);
    this.api.addToCart(product.id, 1).subscribe({
      next: () => {
        this.adding.set(null);
        this.message.set(`${product.name} se agregó al carrito`);
        setTimeout(() => this.message.set(null), 3500);
      },
      error: (err) => {
        this.adding.set(null);
        this.error.set(errorMessage(err, 'No se pudo agregar al carrito'));
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.api.products(this.selected() || undefined).subscribe({
      next: (items) => {
        this.products.set(items);
        this.error.set(null);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(errorMessage(err, 'No se pudo cargar el catálogo'));
      },
    });
  }
}
