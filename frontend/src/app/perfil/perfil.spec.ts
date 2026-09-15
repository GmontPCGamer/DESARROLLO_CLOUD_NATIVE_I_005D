import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Perfil } from './perfil';
import { AuthService } from '../auth/auth.service';
import { StoreApi } from '../core/store.api';

describe('Perfil', () => {
  let component: Perfil;
  let fixture: ComponentFixture<Perfil>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Perfil],
      providers: [
        {
          provide: AuthService,
          useValue: {
            activeAccount: undefined,
            getAccessTokenInfo: () => Promise.resolve(null),
          },
        },
        {
          provide: StoreApi,
          useValue: {
            me: () => throwError(() => new Error('sin sesión')),
            products: () => of([]),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Perfil);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
