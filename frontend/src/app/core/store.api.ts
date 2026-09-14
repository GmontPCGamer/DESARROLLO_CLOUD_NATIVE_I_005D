import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { Cart, NotificationItem, Order, PaymentIntent, Product, Review, ShippingQuote, StockInfo } from './models';

@Injectable({ providedIn: 'root' })
export class StoreApi {
  private readonly base = environment.backendApiUrl;
  private readonly cartChangedSubject = new Subject<void>();
  readonly cartChanged$ = this.cartChangedSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  products(category?: string): Observable<Product[]> {
    const options = category ? { params: { category } } : {};
    return this.http.get<Product[]>(`${this.base}/public/products`, options);
  }

  product(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.base}/public/products/${id}`);
  }

  cart(): Observable<Cart> {
    return this.http.get<Cart>(`${this.base}/cart`);
  }

  addToCart(productId: number, quantity = 1): Observable<Cart> {
    return this.http
      .post<Cart>(`${this.base}/cart/items`, { productId, quantity })
      .pipe(tap(() => this.cartChangedSubject.next()));
  }

  updateQty(productId: number, quantity: number): Observable<Cart> {
    return this.http
      .put<Cart>(`${this.base}/cart/items/${productId}`, { quantity })
      .pipe(tap(() => this.cartChangedSubject.next()));
  }

  removeItem(productId: number): Observable<Cart> {
    return this.http
      .delete<Cart>(`${this.base}/cart/items/${productId}`)
      .pipe(tap(() => this.cartChangedSubject.next()));
  }

  checkout(): Observable<Order> {
    return this.http
      .post<Order>(`${this.base}/orders`, {})
      .pipe(tap(() => this.cartChangedSubject.next()));
  }

  orders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.base}/orders`);
  }

  notifications(): Observable<NotificationItem[]> {
    return this.http.get<NotificationItem[]>(`${this.base}/notifications`);
  }

  unreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.base}/notifications/unread-count`);
  }

  markRead(id: number): Observable<NotificationItem> {
    return this.http.patch<NotificationItem>(`${this.base}/notifications/${id}/read`, {});
  }

  stock(productId: number): Observable<StockInfo> {
    return this.http.get<StockInfo>(`${this.base}/products/${productId}/stock`);
  }

  reviews(productId: number): Observable<Review[]> {
    return this.http.get<Review[]>(`${this.base}/public/products/${productId}/reviews`);
  }

  addReview(productId: number, rating: number, comment: string): Observable<Review> {
    return this.http.post<Review>(`${this.base}/products/${productId}/reviews`, { rating, comment });
  }

  payment(amount: number, method = 'SIMULATED_CARD'): Observable<PaymentIntent> {
    return this.http.post<PaymentIntent>(`${this.base}/payments/intent`, { amount, method });
  }

  shipping(region: string, commune: string): Observable<ShippingQuote> {
    return this.http.post<ShippingQuote>(`${this.base}/shipping/quote`, { region, commune });
  }
}
