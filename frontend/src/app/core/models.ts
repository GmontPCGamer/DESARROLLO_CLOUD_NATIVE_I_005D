export interface Product {
  id: number;
  name: string;
  description: string;
  category: string;
  brand: string;
  price: number;
  imageUrl: string;
  stock: number;
}

export interface CartItem {
  product: Product;
  quantity: number;
  subtotal: number;
}

export interface Cart {
  items: CartItem[];
  total: number;
  totalItems: number;
}

export interface OrderLine {
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
}

export interface Order {
  id: number;
  status: string;
  subtotal: number;
  shippingCost: number;
  total: number;
  shippingService: string | null;
  shippingRegion: string | null;
  shippingCommune: string | null;
  paymentId: string | null;
  paymentMethod: string | null;
  createdAt: string;
  lines: OrderLine[];
}

export interface NotificationItem {
  id: number;
  title: string;
  message: string;
  type: string;
  read: boolean;
  createdAt: string;
}

export interface StockInfo {
  productId: number;
  available: number;
  inStock: boolean;
}

export interface Review {
  id: number;
  productId: number;
  author: string;
  rating: number;
  comment: string;
  createdAt: string;
}

export interface PaymentIntent {
  paymentId: string;
  status: string;
  amount: number;
  method: string;
  createdAt: string;
}

export interface ShippingQuote {
  service: string;
  price: number;
  promise: string;
  estimatedAt: string;
  region: string;
  commune: string;
  free: boolean;
}

export interface CheckoutRequest {
  region: string;
  commune: string;
  paymentMethod: string;
}

export interface CheckoutResponse {
  order: Order;
  payment: PaymentIntent;
  shipping: ShippingQuote;
}

/** Respuesta de GET /api/me: claims del token ya validado por el BFF. */
export interface Me {
  sub: string;
  oid: string | null;
  name: string | null;
  preferred_username: string | null;
  email: string | null;
  tenant: string | null;
  issuer: string | null;
  audience: string[];
  scope: string | null;
  scopes: string[];
  roles: string[];
  authorities: string[];
  isAdmin: boolean;
  issuedAt: string | null;
  expiresAt: string | null;
  tokenVersion: string | null;
}

export interface AdminStockRow {
  product: Product;
  reservable: number;
}

/** Error RFC 7807 que devuelve el BFF. */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
}

export const CATEGORIES = [
  { id: '', label: 'Todos' },
  { id: 'CELULARES', label: 'Celulares' },
  { id: 'COMPUTADORAS', label: 'Computadoras' },
  { id: 'TELEVISORES', label: 'Televisores' },
  { id: 'CONSOLAS', label: 'Consolas' },
  { id: 'ACCESORIOS', label: 'Accesorios' },
] as const;

export const REGIONS = [
  'Metropolitana',
  'Arica y Parinacota',
  'Tarapacá',
  'Antofagasta',
  'Atacama',
  'Coquimbo',
  'Valparaíso',
  "O'Higgins",
  'Maule',
  'Ñuble',
  'Biobío',
  'La Araucanía',
  'Los Ríos',
  'Los Lagos',
  'Aysén',
  'Magallanes',
] as const;

export const PAYMENT_METHODS = [
  { id: 'SIMULATED_CARD', label: 'Tarjeta (simulada)' },
  { id: 'DEBIT', label: 'Débito' },
  { id: 'CREDIT', label: 'Crédito' },
  { id: 'TRANSFER', label: 'Transferencia' },
] as const;
