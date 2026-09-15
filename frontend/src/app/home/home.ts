import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { StoreApi, errorMessage } from '../core/store.api';
import { CATEGORIES, Product } from '../core/models';
import { formatClp } from '../core/money';

@Component({
  imports: [RouterLink],
  selector: 'app-home',
  templateUrl: './home.html',
})
export class Home implements OnInit {
  private readonly api = inject(StoreApi);

  protected readonly featured = signal<Product[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly categories = CATEGORIES.filter((category) => category.id !== '');
  protected readonly formatClp = formatClp;
  protected readonly skeletons = Array.from({ length: 8 }, (_, index) => index);

  ngOnInit(): void {
    this.api.products().subscribe({
      next: (products) => {
        // Destacados: los de menor stock primero (novedades que se agotan) intercalados por categoría.
        const byCategory = new Map<string, Product[]>();
        products.forEach((product) => {
          byCategory.set(product.category, [...(byCategory.get(product.category) ?? []), product]);
        });
        const picks: Product[] = [];
        while (picks.length < Math.min(8, products.length)) {
          for (const list of byCategory.values()) {
            const next = list.shift();
            if (next) picks.push(next);
            if (picks.length >= 8) break;
          }
        }
        this.featured.set(picks);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(errorMessage(err, 'No se pudo cargar el catálogo. ¿Están arriba los microservicios?'));
      },
    });
  }
}
