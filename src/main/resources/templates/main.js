const googleAuthBaseURL = "https://accounts.google.com/o/oauth2/v2/auth";
// IMPORTANTE: Esta URL debe coincidir exactamente con la configurada en Google Console
const redirectURI = encodeURIComponent("http://localhost:8080/auth/grant-code");
const responseType = "code";
const clientID =
  "837113366946-tog1qh9u4qtlpi5h69ri6gbr88rkqdnq.apps.googleusercontent.com";
const scope = encodeURIComponent(
  "https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile openid"
);
const accessType = "offline";

// Armar la URL de autenticación
const googleLoginURL = `${googleAuthBaseURL}?redirect_uri=${redirectURI}&response_type=${responseType}&client_id=${clientID}&scope=${scope}&access_type=${accessType}`;

// Debug: mostrar la URL generada
console.log("Google Login URL:", googleLoginURL);

// Asignar el enlace al botón
document.getElementById("googleLoginBtn").addEventListener("click", () => {
  window.location.href = googleLoginURL;
});
