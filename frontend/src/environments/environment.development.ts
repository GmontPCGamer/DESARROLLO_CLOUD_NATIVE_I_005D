export const environment = {
  production: false,
  msal: {
    clientId: '097bfd84-a8e3-4232-9048-718f4d648efd',
    authority: 'https://login.microsoftonline.com/72fd0b5a-8a6a-4cff-89f6-bde961f7e250',
    apiScope: 'api://nexotech-student-api/access_as_user',
    redirectUri: 'http://localhost:4200/auth',
    postLogoutRedirectUri: 'http://localhost:4200',
  },
  backendApiUrl: 'http://localhost:8080/api',
};