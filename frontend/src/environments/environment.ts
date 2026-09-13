export const environment = {
  production: true,
  msal: {
    clientId: 'REEMPLAZAR_CON_CLIENT_ID',
    authority: 'https://login.microsoftonline.com/REEMPLAZAR_CON_TENANT_ID',
    redirectUri: 'https://REEMPLAZAR_DOMINIO/auth',
    postLogoutRedirectUri: 'https://REEMPLAZAR_DOMINIO',
  },
  backendApiUrl: 'https://REEMPLAZAR_API_GATEWAY.execute-api.us-east-1.amazonaws.com/v1',
};