# Integración de Retrofit con Flask API

## Configuración Completada

Se ha integrado exitosamente Retrofit en la aplicación Android para consumir los servicios REST del proyecto Flask.

## Estructura Implementada

### 1. Modelos de Datos (`data/models/`)
- `User.kt` - Modelo de usuarios
- `Product.kt` - Modelo de productos  
- `Cart.kt` - Modelo de carrito

### 2. API Service (`data/api/`)
- `ApiService.kt` - Interfaz con todos los endpoints
- `RetrofitClient.kt` - Configuración de Retrofit
- `NetworkResult.kt` - Wrapper para respuestas

### 3. Repositorios (`data/repository/`)
- `ProductRepository.kt` - Gestión de productos
- `UserRepository.kt` - Gestión de usuarios

### 4. ViewModels Actualizados
- `HomeViewModel.kt` - Carga productos desde API
- `DashboardViewModel.kt` - Carga usuarios desde API

### 5. UI Actualizada
- `HomeFragment.kt` - Muestra lista de productos
- `DashboardFragment.kt` - Muestra lista de usuarios
- Adapters para RecyclerView
- Layouts con ProgressBar y manejo de errores

## Cómo Ejecutar

### 1. Iniciar el servidor Flask
```bash
cd /Users/alancruzmendez/projects/PruebaFlask
python app.py
```
El servidor correrá en `http://localhost:5002`

### 2. Ejecutar la aplicación Android

#### Para Emulador:
La configuración actual usa `10.0.2.2:5002` que es la IP especial para acceder a localhost desde el emulador Android.

#### Para Dispositivo Físico:
1. Encuentra tu IP local (en Mac: `ifconfig | grep inet`)
2. Actualiza `BASE_URL` en `RetrofitClient.kt` con tu IP
3. Asegúrate que tu dispositivo esté en la misma red

### 3. Compilar y ejecutar
```bash
./gradlew installDebug
```

## Endpoints Disponibles

### Productos
- GET `/products` - Obtener todos los productos
- GET `/products/{id}` - Obtener producto por ID
- POST `/products` - Crear producto
- PUT `/products/{id}` - Actualizar producto
- DELETE `/products/{id}` - Eliminar producto

### Usuarios
- GET `/users` - Obtener todos los usuarios
- GET `/users/{id}` - Obtener usuario por ID
- POST `/users` - Crear usuario
- PUT `/users/{id}` - Actualizar usuario
- DELETE `/users/{id}` - Eliminar usuario

## Permisos Añadidos
- `INTERNET` - Para realizar peticiones HTTP
- `ACCESS_NETWORK_STATE` - Para verificar conectividad
- `usesCleartextTraffic="true"` - Para permitir HTTP (no HTTPS)

## Dependencias Añadidas
```kotlin
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
```

## Notas Importantes
- La aplicación maneja estados de carga, éxito y error
- Los datos se actualizan automáticamente al abrir cada pantalla
- Se muestra un Toast al seleccionar un producto o usuario
- El logging está habilitado para debug de peticiones HTTP