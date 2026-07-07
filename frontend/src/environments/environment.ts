export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  rtcIceServers: [
    { urls: 'stun:stun.cloudflare.com:3478' },
    { urls: 'stun:stun.l.google.com:19302' },
  ] as RTCIceServer[],
};
