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
  total: number;
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
}

export const CATEGORIES = [
  { id: '', label: 'Todos' },
  { id: 'CELULARES', label: 'Celulares' },
  { id: 'COMPUTADORAS', label: 'Computadoras' },
  { id: 'TELEVISORES', label: 'Televisores' },
  { id: 'CONSOLAS', label: 'Consolas' },
  { id: 'ACCESORIOS', label: 'Accesorios' },
] as const;
