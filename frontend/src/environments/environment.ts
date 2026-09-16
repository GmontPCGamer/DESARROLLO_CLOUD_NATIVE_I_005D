export const environment = {
  production: true,
  msal: {
    // App "NexoTech Demo SPA" (owner: fe.ardiles) — redirect sslip.io ya configurado
    clientId: 'f7d7e5dd-430c-4adb-9348-9ecd974b220c',
    authority: 'https://login.microsoftonline.com/72fd0b5a-8a6a-4cff-89f6-bde961f7e250',
    apiScope: 'api://nexotech-demo-api/access_as_user',
    redirectUri: 'https://18-211-7-130.sslip.io/auth',
    postLogoutRedirectUri: 'https://18-211-7-130.sslip.io',
  },
  backendApiUrl: 'https://ourd5f7qr1.execute-api.us-east-1.amazonaws.com/dev/api',
};
