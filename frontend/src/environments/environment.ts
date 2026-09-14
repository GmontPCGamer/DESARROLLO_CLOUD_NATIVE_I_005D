export const environment = {
  production: true,
  msal: {
    clientId: '097bfd84-a8e3-4232-9048-718f4d648efd',
    authority: 'https://login.microsoftonline.com/72fd0b5a-8a6a-4cff-89f6-bde961f7e250',
    apiScope: 'api://nexotech-student-api/access_as_user',
    redirectUri: 'https://REEMPLAZAR_DOMINIO/auth',
    postLogoutRedirectUri: 'https://REEMPLAZAR_DOMINIO',
  },
  backendApiUrl: 'https://REEMPLAZAR_API_GATEWAY.execute-api.us-east-1.amazonaws.com/v1',
};