# HCL Domino - MS Teams SSO sample
This Domino app helps with single-sign on between Microsoft Teams and HCL Domino. Currently, it supports only multi-server SSO Domino configuration.

## Description

Microsft Teams allow apps to get AAD token for current user using Microosft Teams SDK. This token contains user information, so we can extract e.g. the upn and use it to authenticate the user on Domino server. 

Application validates the AAD token, finds related user document in Domino directory, generates LTPA token and redirects the user to target database.

If the user is already authenticated with Domino server, it just redirects the user directly.

## Deployment
 - install app on web enabled Domino server
 - sign the app with ID that has access to the LTPA token configuration
 - create a new AAD app registration for Teams SSO
 - create a new Teams app for the app
 -- configure personal tab with URL syntax `{app_url}/auth.xsp?dest={target_url}`
 -- configure configurable tab with URL `{app_url}/config`
 - configure the Domino app settings
 -- LTPA configuration
 -- SameSite setting 
 - install the Teams app locally for testing and then publish it

### Known limitations
 - SSO is implemented only for multi-server Domino SSO using LTPA token
 - AAD upn is used as search key in ($Users) view of main Domino Address book, other logic needs code customizations

### Notes
 - SameSite cookie must be set to None to work in Teams web client

### References
 - XPages snippet to generate LTPA token from Sedar Basegmez - https://openntf.org/XSnippets.nsf/snippet.xsp?id=ltpatoken-generator-for-multi-server-sso-configurations
 - Microsoft Teams SSO documentation - https://docs.microsoft.com/en-us/microsoftteams/platform/tabs/how-to/authentication/auth-aad-sso
 - Auth0 Java JWT - https://github.com/auth0/java-jwt
 
