# Sistema de Chat Cliente-Admin

## Descripción
Sistema de chat en tiempo real que permite a los clientes hacer preguntas y a los administradores responder usando WebSockets.

## Arquitectura

### WebSocket Endpoints
- **Conexión:** `ws://localhost:8080/ws`
- **Enviar mensaje:** `/app/chat.sendMessage`
- **Suscribirse a QA:** `/topic/qa/{qaId}`

### REST Endpoints

#### Chat
- `POST /chat/send` - Enviar mensaje
- `GET /chat/history/{qaId}` - Obtener historial de chat
- `GET /chat/qa/client/{clientId}` - Obtener QAs de un cliente
- `GET /chat/qa/unresponded` - Obtener QAs sin responder

#### QA
- `POST /qa/create` - Crear nueva QA
- `GET /qa/{qaId}` - Obtener QA específica
- `GET /qa/client/{clientId}` - Obtener QAs de un cliente
- `GET /qa/unresponded` - Obtener QAs sin responder
- `PATCH /qa/{qaId}/respond` - Marcar QA como respondida

## Flujo de Uso

### 1. Cliente crea una pregunta
```javascript
// Crear nueva QA
fetch('/qa/create?message=¿Cómo hago la postura del perro?&clientId=1', {
  method: 'POST'
});

// O enviar mensaje directamente
fetch('/chat/send', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    message: '¿Cómo hago la postura del perro?',
    clientId: 1
  })
});
```

### 2. Conectar WebSocket
```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
  // Suscribirse a un QA específico
  stompClient.subscribe('/topic/qa/1', function(message) {
    const chatMessage = JSON.parse(message.body);
    console.log('Nuevo mensaje:', chatMessage);
  });
});
```

### 3. Enviar mensaje por WebSocket
```javascript
stompClient.send('/app/chat.sendMessage', {}, JSON.stringify({
  message: '¿Puedes explicar más?',
  clientId: 1,
  qaId: 1
}));
```

### 4. Admin responde
```javascript
// Admin responde
stompClient.send('/app/chat.sendMessage', {}, JSON.stringify({
  message: 'Para hacer la postura del perro...',
  adminId: 1,
  qaId: 1
}));
```

## Estructura de Datos

### ChatMessageDto
```json
{
  "id": 1,
  "content": "¿Cómo hago la postura del perro?",
  "timestamp": "2024-01-15T10:30:00Z",
  "senderType": "CLIENT",
  "qaId": 1,
  "senderId": 1,
  "senderName": "Juan Pérez"
}
```

### QAResponseDto
```json
{
  "id": 1,
  "message": "¿Cómo hago la postura del perro?",
  "createdAt": "2024-01-15T10:30:00Z",
  "isResponded": true,
  "clientId": 1,
  "clientName": "Juan Pérez",
  "chatMessages": [...]
}
```

## Configuración

El sistema está configurado con:
- WebSocket habilitado en `/ws`
- STOMP como protocolo de mensajería
- Destinos: `/topic` (broadcast), `/queue` (privado), `/user` (usuario específico)
- CORS habilitado para desarrollo

## Notas Importantes

1. **Seguridad:** Implementar autenticación JWT para proteger endpoints
2. **Validación:** Agregar validaciones en DTOs
3. **Persistencia:** Los mensajes se guardan automáticamente en la base de datos
4. **Tiempo Real:** Los mensajes se envían inmediatamente a todos los suscriptores del QA 