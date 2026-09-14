import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Perfil } from './perfil';
import { MsalService } from '@azure/msal-angular';

describe('Perfil', () => {
  let component: Perfil;
  let fixture: ComponentFixture<Perfil>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Perfil],
      providers: [
        {
          provide: MsalService,
          useValue: {
            instance: {
              getActiveAccount: () => null,
              getAllAccounts: () => [],
            },
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
