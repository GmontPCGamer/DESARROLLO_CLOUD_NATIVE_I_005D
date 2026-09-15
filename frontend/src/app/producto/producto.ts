import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Product, Review } from '../core/models';
import { StoreApi, errorMessage } from '../core/store.api';
import { formatClp } from '../core/money';
import { AuthService } from '../auth/auth.service';

@Component({
  imports: [RouterLink, DatePipe],
  selector: 'app-producto',
  templateUrl: './producto.html',
})
export class Producto implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(StoreApi);
  private readonly auth = inject(AuthService);

  protected readonly product = signal<Product | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly formatClp = formatClp;

  protected readonly quantity = signal(1);
  protected readonly adding = signal(false);

  protected readonly reviews = signal<Review[]>([]);
  protected readonly reviewText = signal('');
  protected readonly reviewRating = signal(5);
  protected readonly reviewError = signal<string | null>(null);
  protected readonly publishing = signal(false);
  protected readonly stars = [1, 2, 3, 4, 5];

  protected readonly averageRating = computed(() => {
    const items = this.reviews();
    if (items.length === 0) return 0;
    return Math.round((items.reduce((sum, review) => sum + review.rating, 0) / items.length) * 10) / 10;
  });

  protected readonly roundedAverage = computed(() => Math.round(this.averageRating()));
  protected readonly maxQty = computed(() => Math.max(1, Math.min(this.product()?.stock ?? 1, 10)));

  protected readonly isAuthenticated = computed(() => this.auth.isAuthenticated());

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = Number(params.get('id'));
      this.load(id);
    });
  }

  protected changeQty(delta: number): void {
    const max = this.maxQty();
    this.quantity.update((current) => Math.min(max, Math.max(1, current + delta)));
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
    this.adding.set(true);
    this.error.set(null);
    this.api.addToCart(item.id, this.quantity()).subscribe({
      next: () => {
        this.adding.set(false);
        this.message.set(`${this.quantity()} × ${item.name} en tu carrito`);
        setTimeout(() => this.message.set(null), 3500);
      },
      error: (err) => {
        this.adding.set(false);
        this.error.set(errorMessage(err, 'No se pudo agregar al carrito'));
      },
    });
  }

  protected publishReview(): void {
    const item = this.product();
    if (!item) {
      return;
    }
    if (!this.auth.activeAccount) {
      this.auth.login();
      return;
    }
    if (!this.reviewText().trim()) {
      this.reviewError.set('Escribe una opinión antes de publicar');
      return;
    }
    this.publishing.set(true);
    this.reviewError.set(null);
    this.api.addReview(item.id, this.reviewRating(), this.reviewText().trim()).subscribe({
      next: (review) => {
        this.publishing.set(false);
        this.reviews.update((items) => [review, ...items]);
        this.reviewText.set('');
        this.reviewRating.set(5);
        this.message.set('Gracias por compartir tu experiencia');
        setTimeout(() => this.message.set(null), 3500);
      },
      error: (err) => {
        this.publishing.set(false);
        this.reviewError.set(errorMessage(err, 'No se pudo publicar la reseña'));
      },
    });
  }

  protected starText(rating: number): string {
    return '★'.repeat(rating) + '☆'.repeat(5 - rating);
  }

  private load(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.quantity.set(1);
    this.api.product(id).subscribe({
      next: (item) => {
        this.product.set(item);
        this.loading.set(false);
        this.api.reviews(item.id).subscribe({ next: (reviews) => this.reviews.set(reviews) });
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(errorMessage(err, 'Producto no encontrado'));
      },
    });
  }
}
