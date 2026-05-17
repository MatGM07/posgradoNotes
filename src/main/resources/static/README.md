# Frontend - Sistema de Notas

## 📋 Descripción

Frontend limpio y moderno para el Sistema de Notas construido con HTML5, CSS3 y JavaScript vanilla.

## 🎯 Características

### Para todos los usuarios:
- ✅ Login seguro
- ✅ Ver perfil del usuario actual
- ✅ Visualizar notas (diferentes según el rol)
- ✅ Cerrar sesión

### Para Profesores (PROFESOR):
- ✅ Crear notas
- ✅ Editar notas propias
- ✅ Eliminar notas propias
- ✅ Asignar notas a estudiantes
- ✅ Agregar calificaciones
- ✅ Ver lista de estudiantes registrados

### Para Estudiantes (ESTUDIANTE):
- ✅ Ver notas asignadas
- ✅ Ver detalles de notas
- ✅ Ver calificaciones

## 🚀 Cómo usar

### 1. Acceder a la aplicación
```
http://localhost:8080/
```

### 2. Login
- Ingresa tu usuario y contraseña
- Los datos se almacenan de forma segura en sesión

### 3. Navegar
- **Mis Notas**: Ver todas las notas (profesor: las que creó, estudiante: las asignadas)
- **Crear Nota** (solo profesor): Formulario para crear una nueva nota
- **Estudiantes** (solo profesor): Tabla con todos los estudiantes registrados

## 📁 Estructura de archivos

```
src/main/resources/static/
├── index.html          # Página principal
├── css/
│   └── style.css      # Estilos (responsive, moderno)
└── js/
    └── app.js         # Lógica de la aplicación
```

## 🔐 Autenticación

El frontend utiliza **Bearer Token** (JWT) para autenticación. El token se almacena en `localStorage` y se incluye en todas las peticiones al API:

```javascript
Authorization: Bearer {token}
```

## 🎨 Diseño

- **Moderno**: Colores limpios con gradientes
- **Responsive**: Funciona en móvil, tablet y desktop
- **Accesible**: Contraste adecuado y navegación clara
- **Rápido**: Sin dependencias externas (vanilla JS)

## 🔧 Personalización

### Colores (variables CSS)
En `css/style.css`, líneas 6-16:
```css
--primary-color: #3498db;     /* Color principal */
--danger-color: #e74c3c;      /* Color de eliminar */
--success-color: #27ae60;     /* Color de éxito */
--warning-color: #f39c12;     /* Color de advertencia */
```

### Endpoint de API
En `js/app.js`, línea 16:
```javascript
const API_BASE = '';  // Cambiar si API está en otra URL
```

## 📌 Notas importantes

1. **Autenticación**: El token se valida en cada petición
2. **Autorización**: El servidor valida permisos (profesor/estudiante)
3. **Validación**: El formulario valida datos en cliente y servidor
4. **Errores**: Se muestran mensajes claros en caso de error
5. **Responsive**: Funciona en dispositivos móviles

## 🔄 Flujo de una nota

### Crear (Profesor)
1. Hace clic en "Crear Nota"
2. Completa el formulario
3. Selecciona un estudiante
4. Envía y se crea la nota
5. Se actualiza la lista automáticamente

### Ver (Profesor/Estudiante)
1. Ve la lista de notas en el dashboard
2. Hace clic para ver detalles en modal
3. Si es profesor, puede editar o eliminar

### Editar (Profesor)
1. Abre la nota en el modal
2. Hace clic en "Editar"
3. Modifica los datos
4. Guarda los cambios
5. Se actualiza la lista automáticamente

### Eliminar (Profesor)
1. Abre la nota en el modal
2. Hace clic en "Eliminar"
3. Confirma la acción
4. Se elimina y actualiza la lista

## 📱 Compatibilidad

- ✅ Chrome (últimas versiones)
- ✅ Firefox (últimas versiones)
- ✅ Safari (últimas versiones)
- ✅ Edge (últimas versiones)
- ✅ Dispositivos móviles (iOS, Android)

## 🐛 Troubleshooting

### No se carga la aplicación
- Verifica que el servidor Spring Boot esté corriendo
- Comprueba que `/index.html` sea accesible

### Error "Token inválido"
- Limpia el localStorage: `localStorage.clear()`
- Vuelve a hacer login

### Las notas no se cargan
- Revisa la consola del navegador (F12)
- Verifica que el endpoint `/notes` esté disponible

### Cambios no se guardan
- Comprueba que el servidor responda correctamente
- Verifica los permisos del usuario (profesor/estudiante)

## 🔗 Endpoints utilizados

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/me` | Obtener usuario actual |
| GET | `/notes` | Listar mis notas |
| GET | `/notes/{id}` | Obtener una nota |
| POST | `/notes` | Crear nota (profesor) |
| PUT | `/notes/{id}` | Actualizar nota (profesor) |
| DELETE | `/notes/{id}` | Eliminar nota (profesor) |
| GET | `/students` | Listar estudiantes (profesor) |
| POST | `/login` | Autenticación |

## 💡 Tips

- Usa tab o Enter para navegar el formulario más rápido
- El modal se cierra haciendo clic fuera de él o en la X
- La lista de notas se actualiza automáticamente después de guardar
- Los estudiantes solo ven sus notas asignadas

## 📧 Soporte

Para reportar problemas o sugerir mejoras, contacta al equipo de desarrollo.
