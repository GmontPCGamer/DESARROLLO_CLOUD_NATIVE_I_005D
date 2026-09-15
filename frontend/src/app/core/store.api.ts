import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, Subject, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AdminStockRow,
  Cart,
  CheckoutRequest,
  CheckoutResponse,
  Me,
  NotificationItem,
  Order,
  PaymentIntent,
  ProblemDetail,
  Product,
  Review,
  ShippingQuote,
  StockInfo,
} from './models';

/**
 * Cliente HTTP hacia el BFF. Las rutas privadas reciben el Bearer token
 * automáticamente gracias al MsalInterceptor (ver auth.config.ts).
 */
@Injectable({ providedIn: 'root' })
export class StoreApi {
  private readonly base = environment.backendApiUrl;
  private readonly cartChangedSubject = new Subject<void>();
  /** Se emite cada vez que el carrito cambia, para refrescar el contador del header. */
  readonly cartChanged$ = this.cartChangedSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  // ---- Público ----

  products(category?: string): Observable<Product[]> {
    const options = category ? { params: { category } } : {};
    return this.http.get<Product[]>(`${this.base}/public/products`, options);
  }

  product(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.base}/public/products/${id}`);
  }

  reviews(productId: number): Observable<Review[]> {
    return this.http.get<Review[]>(`${this.base}/public/products/${productId}/reviews`);
  }

  // ---- Sesión ----

  me(): Observable<Me> {
    return this.http.get<Me>(`${this.base}/me`);
  }

  // ---- Carrito ----

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

  clearCart(): Observable<Cart> {
    return this.http.delete<Cart>(`${this.base}/cart`).pipe(tap(() => this.cartChangedSubject.next()));
  }

  // ---- Checkout ----

  shipping(region: string, commune: string): Observable<ShippingQuote> {
    return this.http.post<ShippingQuote>(`${this.base}/shipping/quote`, { region, commune });
  }

  payment(amount: number, method = 'SIMULATED_CARD'): Observable<PaymentIntent> {
    return this.http.post<PaymentIntent>(`${this.base}/payments/intent`, { amount, method });
  }

  /** El BFF cotiza despacho, autoriza el pago y confirma la orden en un solo paso. */
  checkout(request: CheckoutRequest): Observable<CheckoutResponse> {
    return this.http
      .post<CheckoutResponse>(`${this.base}/orders`, request)
      .pipe(tap(() => this.cartChangedSubject.next()));
  }

  orders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.base}/orders`);
  }

  order(id: number): Observable<Order> {
    return this.http.get<Order>(`${this.base}/orders/${id}`);
  }

  // ---- Notificaciones ----

  notifications(): Observable<NotificationItem[]> {
    return this.http.get<NotificationItem[]>(`${this.base}/notifications`);
  }

  unreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.base}/notifications/unread-count`);
  }

  markRead(id: number): Observable<NotificationItem> {
    return this.http.patch<NotificationItem>(`${this.base}/notifications/${id}/read`, {});
  }

  markAllRead(): Observable<{ updated: number }> {
    return this.http.patch<{ updated: number }>(`${this.base}/notifications/read-all`, {});
  }

  // ---- Inventario y reseñas ----

  stock(productId: number): Observable<StockInfo> {
    return this.http.get<StockInfo>(`${this.base}/products/${productId}/stock`);
  }

  addReview(productId: number, rating: number, comment: string): Observable<Review> {
    return this.http.post<Review>(`${this.base}/products/${productId}/reviews`, { rating, comment });
  }

  // ---- Administración (ROLE_ADMIN) ----

  adminInventory(): Observable<AdminStockRow[]> {
    return this.http.get<AdminStockRow[]>(`${this.base}/admin/inventory`);
  }

  restock(productId: number, quantity: number): Observable<AdminStockRow> {
    return this.http.post<AdminStockRow>(`${this.base}/admin/products/${productId}/restock`, { quantity });
  }
}

/** Extrae un mensaje legible desde un error HTTP del BFF (ProblemDetail) o de MSAL. */
export function errorMessage(error: unknown, fallback = 'Ocurrió un error inesperado'): string {
  if (error instanceof HttpErrorResponse) {
    const problem = error.error as ProblemDetail | null;
    if (problem && typeof problem === 'object' && problem.detail) {
      return problem.detail;
    }
    if (error.status === 0) {
      return 'No se pudo contactar al servidor. ¿Está levantado el BFF en el puerto 8080?';
    }
    if (error.status === 401) {
      return 'Tu sesión expiró o el token no es válido. Inicia sesión nuevamente.';
    }
    if (error.status === 403) {
      return 'No tienes permisos para esta acción.';
    }
    if (error.status === 503) {
      return 'Un microservicio no está disponible en este momento.';
    }
    return `${fallback} (HTTP ${error.status})`;
  }
  if (error && typeof error === 'object' && 'errorMessage' in error) {
    return String((error as { errorMessage: string }).errorMessage);
  }
  return fallback;
}
