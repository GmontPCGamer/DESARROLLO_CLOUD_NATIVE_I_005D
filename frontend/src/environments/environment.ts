export const environment = {
  production: true,
  msal: {
    // App en NextTechDemo (multi-tenant). Authority = Duoc para que cualquier @duocuc.cl pueda entrar.
    clientId: 'b541332a-4305-4111-84f1-b5584ade7d44',
    authority: 'https://login.microsoftonline.com/72fd0b5a-8a6a-4cff-89f6-bde961f7e250',
    apiScope: 'api://b541332a-4305-4111-84f1-b5584ade7d44/access_as_user',
    redirectUri: 'https://armed-merchants-optimization-wider.trycloudflare.com/auth',
    postLogoutRedirectUri: 'https://armed-merchants-optimization-wider.trycloudflare.com',
  },
  backendApiUrl: 'https://ourd5f7qr1.execute-api.us-east-1.amazonaws.com/dev/api',
};
