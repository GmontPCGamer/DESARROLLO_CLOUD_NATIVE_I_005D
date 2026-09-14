import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { StoreApi } from '../core/store.api';
import { Product } from '../core/models';
import { formatClp } from '../core/money';

@Component({
  imports: [RouterLink],
  selector: 'app-home',
  templateUrl: './home.html',
})
export class Home implements OnInit {
  private readonly api = inject(StoreApi);

  protected readonly featured = signal<Product[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly formatClp = formatClp;

  ngOnInit(): void {
    this.api.products().subscribe({
      next: (products) => this.featured.set(products.slice(0, 8)),
      error: () => this.error.set('No se pudo cargar el catálogo. ¿Están arriba los microservicios?'),
    });
  }
}
