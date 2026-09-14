import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { App } from './app';
import { AuthService } from './auth/auth.service';
import { StoreApi } from './core/store.api';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            trackAuthenticationStatus: () => of({}),
            refreshAuthenticationState: () => undefined,
            activeAccount: undefined,
            login: () => undefined,
            logout: () => undefined,
          },
        },
        {
          provide: StoreApi,
          useValue: {
            cart: () => of({ items: [], total: 0, totalItems: 0 }),
            unreadCount: () => of({ count: 0 }),
          },
        },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the store brand', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Pedidos360');
  });
});
