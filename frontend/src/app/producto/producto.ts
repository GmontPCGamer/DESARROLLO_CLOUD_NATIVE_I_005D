import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Product, Review } from '../core/models';
import { StoreApi } from '../core/store.api';
import { formatClp } from '../core/money';
import { AuthService } from '../auth/auth.service';

@Component({
  imports: [RouterLink],
  selector: 'app-producto',
  templateUrl: './producto.html',
})
export class Producto implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(StoreApi);
  private readonly auth = inject(AuthService);

  protected readonly product = signal<Product | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly formatClp = formatClp;
  protected readonly reviews = signal<Review[]>([]);
  protected readonly reviewText = signal('');
  protected readonly reviewRating = signal(5);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.product(id).subscribe({
      next: (item) => {
        this.product.set(item);
        this.api.reviews(item.id).subscribe({ next: (reviews) => this.reviews.set(reviews) });
      },
      error: () => this.error.set('Producto no encontrado'),
    });
  }

  protected add(): void {
    const item = this.product();
    if (!item) {
      return;
    }
    if (!this.auth.activeAccount) {
      this.auth.login();
      return;
    }
    this.api.addToCart(item.id, 1).subscribe({
      next: () => this.message.set('Agregado al carrito'),
      error: () => this.error.set('No se pudo agregar al carrito'),
    });
  }

  protected publishReview(): void {
    const item = this.product();
    if (!item || !this.auth.activeAccount) {
      this.auth.login();
      return;
    }
    if (!this.reviewText().trim()) {
      this.error.set('Escribe una opinión antes de publicar');
      return;
    }
    this.api.addReview(item.id, this.reviewRating(), this.reviewText()).subscribe({
      next: (review) => {
        this.reviews.update((items) => [review, ...items]);
        this.reviewText.set('');
        this.message.set('Gracias por compartir tu experiencia');
      },
      error: () => this.error.set('No se pudo publicar la reseña'),
    });
  }
}
