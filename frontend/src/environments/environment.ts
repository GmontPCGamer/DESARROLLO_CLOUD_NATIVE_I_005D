export const environment = {
  production: true,
  msal: {
    // Tenant NextTechDemo (f2e0852e-…) — App "NexoTech Demo SPA"
    clientId: 'b541332a-4305-4111-84f1-b5584ade7d44',
    authority: 'https://login.microsoftonline.com/f2e0852e-19c3-4785-baa7-f24347e3ccea',
    apiScope: 'api://nexotech-demo-api/access_as_user',
    redirectUri: 'https://departments-intl-stick-homework.trycloudflare.com/auth',
    postLogoutRedirectUri: 'https://departments-intl-stick-homework.trycloudflare.com',
  },
  backendApiUrl: 'https://ourd5f7qr1.execute-api.us-east-1.amazonaws.com/dev/api',
};
