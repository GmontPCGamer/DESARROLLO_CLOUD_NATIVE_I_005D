export const environment = {
  production: false,
  msal: {
    clientId: 'REEMPLAZAR_CON_CLIENT_ID',
    authority: 'https://login.microsoftonline.com/REEMPLAZAR_CON_TENANT_ID',
    redirectUri: 'http://localhost:4200/auth',
    postLogoutRedirectUri: 'http://localhost:4200',
  },
  backendApiUrl: 'http://localhost:8080/api',
};