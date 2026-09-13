import { Component, signal } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo } from '@azure/msal-browser';

@Component({
  imports: [],
  selector: 'app-perfil',
  styleUrl: './perfil.css',
  templateUrl: './perfil.html',
})
export class Perfil {
  protected readonly account = signal<AccountInfo | null>(null);

  constructor(private readonly msalService: MsalService) {
    this.account.set(
      this.msalService.instance.getActiveAccount() ?? this.msalService.instance.getAllAccounts()[0] ?? null,
    );
  }
}