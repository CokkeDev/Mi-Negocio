<div align="center">

<img src="docs/icono.svg" width="128" alt="Ícono de Mi Negocio">

# Mi Negocio

**Stock, ventas y ganancias de tu kiosko, en tu teléfono y sin internet.**

Una app Android simple para llevar el inventario por lotes, vender escaneando códigos de barras
y saber cuánto ganas de verdad, sin crear cuentas y sin que tus datos salgan del teléfono.

![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Room](https://img.shields.io/badge/Room-SQLite-00506E)
![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)
![Offline](https://img.shields.io/badge/funciona-sin%20internet-FFB951)
![Tests](https://img.shields.io/badge/tests-43%20pasando-2E7D32)

</div>

---

## 📱 Capturas

| Inicio | Venta | Detalle del producto |
|:---:|:---:|:---:|
| <img src="docs/capturas/inicio.png" width="230" alt="Pantalla de inicio con ventas y ganancia del día"> | <img src="docs/capturas/venta.png" width="230" alt="Venta con escáner de códigos y boleta"> | <img src="docs/capturas/detalle.png" width="230" alt="Detalle del producto con sus lotes"> |
| **Reportes** | **Gastos** | **Tema oscuro** |
| <img src="docs/capturas/reportes.png" width="230" alt="Reportes con ganancia neta y gráfico por mes"> | <img src="docs/capturas/gastos.png" width="230" alt="Gastos del mes por categoría"> | <img src="docs/capturas/tema-oscuro.png" width="230" alt="Pantalla de inicio en tema oscuro"> |

## 💡 El problema

Las apps de inventario suelen pedir una cuenta, una suscripción o conexión permanente.
Un kiosko de barrio solo necesita algo rápido: **anotar lo que compra, cobrar lo que vende y saber si al final del mes ganó o perdió**.
Mi Negocio hace exactamente eso y todo queda guardado en el propio teléfono.

## ✨ Funcionalidades

- **Productos**: nombre, precio de venta, código de barras (escaneado con la cámara) y foto opcional. Avisa si el producto ya existe y permite reactivar los eliminados.
- **Lotes con vencimiento**: cada compra se ingresa como un lote con su precio de compra, unidades y fecha de vencimiento.
- **Ventas con escáner**: apuntas la cámara al código y el producto se suma a la boleta. Los productos sin código se eligen de una lista.
- **Descuento FEFO**: al vender, el stock sale primero del lote que vence antes (*First Expired, First Out*).
- **Ventas seguras**: cada venta se guarda en una transacción. Si falta stock, no queda nada a medias.
- **Merma**: registra lo vencido, dañado o perdido, valorizado a precio de compra.
- **Gastos**: luz, arriendo, transporte, insumos… agrupados por categoría y por semana.
- **Reportes**: ganancia neta (ventas − costo − gastos − merma), gráfico de los últimos 6 meses y productos más vendidos.
- **Alertas**: productos por vencer y sin stock, con días de aviso configurables.
- **Respaldo**: exporta e importa la base de datos a un archivo (Descargas, Drive, pendrive…). Revisa que el archivo sea un respaldo válido antes de restaurarlo.
- **Tema claro y oscuro**, según el sistema o a elección.

## 🔒 Privacidad

- **Sin cuentas y sin internet**: la app ni siquiera tiene el permiso de internet (`INTERNET` se quita del manifiesto).
- El lector de códigos usa el modelo de ML Kit **incluido en la app**, así que funciona sin conexión.
- El único permiso que pide es la **cámara**, para escanear códigos y sacar fotos de productos.
- Todos los datos viven en una base SQLite dentro del almacenamiento privado de la app.

## 🛠️ Tecnologías

| Área | Herramienta |
|---|---|
| Lenguaje | Kotlin 2.3 |
| Interfaz | Jetpack Compose · Material 3 |
| Navegación | Navigation 3 |
| Base de datos | Room 2.8 (SQLite) con migraciones automáticas |
| Cámara y escáner | CameraX · ML Kit Barcode Scanning (modelo incluido) |
| Arquitectura | MVVM · repositorios · inyección de dependencias manual |
| Tests | JUnit 4 · Robolectric · kotlinx-coroutines-test |

## 🧱 Arquitectura

El proyecto separa la interfaz, las reglas del negocio y el acceso a datos (MVVM en capas):

```mermaid
flowchart LR
    UI["presentation<br/>Pantallas Compose + ViewModels"] --> D["domain<br/>Modelos, reglas (FEFO) e interfaces"]
    UI --> Core["core<br/>Componentes UI, formato, fechas"]
    Data["data<br/>Room, DAOs, repositorios, respaldo"] --> D
    Data --> DB[("SQLite")]
```

```
app/src/main/kotlin/cl/miproyecto/minegocio/
├── core/           # Componentes de UI reutilizables, íconos, escáner, utilidades
├── data/           # Entidades y DAOs de Room, repositorios, preferencias y respaldo
├── domain/         # Modelos del negocio, interfaces de repositorios y casos de uso (FEFO)
├── presentation/   # Pantallas y ViewModels: productos, lotes, ventas, merma, gastos, reportes...
└── ui/theme/       # Colores, tipografía y tema claro/oscuro
```

## 🗃️ Modelo de datos

```mermaid
classDiagram
  class Producto {
    +Int id_producto PK
    +String sku UK
    +String nombre
    +String nombre_normalizado UK
    +String URL_imagen
    +Int precio_venta
    +Boolean activo
  }
  class Lote {
    +Int id_lote PK
    +Int id_producto FK
    +Int precio_compra
    +Date fecha_ingreso
    +Date fecha_vencimiento nullable
    +Int unidades_iniciales
    +Int unidades_disponibles
  }
  class Perdida {
    +Int id_perdida PK
    +Int id_lote FK
    +Int cantidad
    +String motivo
    +String nota nullable
    +Date fecha
  }
  class Venta {
    +Int id_venta PK
    +Date fecha
    +Int total
  }
  class DetalleVenta {
    +Int id_detalle PK
    +Int id_venta FK
    +Int id_lote FK
    +Int cantidad
    +Int precio_unitario
  }
  class Gasto {
    +Int id_gasto PK
    +String descripcion
    +Int monto
    +String categoria
    +Date fecha
  }

  Producto "1" --> "*" Lote : tiene
  Lote "1" --> "*" Perdida : registra
  Lote "1" --> "*" DetalleVenta : se vende en
  Venta "1" --> "*" DetalleVenta : contiene
```

> La ganancia se calcula con el **precio de compra del lote** del que salió cada unidad,
> por eso cada línea de venta guarda el lote (`id_lote`) y no solo el producto.

## 🔄 Flujos principales

<details>
<summary><b>Registro de producto</b></summary>

```mermaid
sequenceDiagram
    actor Usuario
    participant Scanner as EscanerCamara
    participant Screen as AgregarProductoScreen
    participant VM as ProductoViewModel
    participant Repo as ProductoRepository
    participant DAO as ProductoDAO
    participant DB as SQLite

    Usuario->>Screen: Completar nombre, precio e imagen
    opt Producto con código de barras
        Usuario->>Screen: Presionar escanear SKU
        Screen->>Scanner: Activar cámara
        Scanner-->>Screen: Código detectado
    end
    Usuario->>Screen: Presionar Agregar
    Screen->>VM: AgregarProducto(producto)
    VM->>VM: Validar datos y normalizar nombre
    VM->>Repo: BuscarDuplicado(nombreNormalizado, sku)
    Repo->>DAO: BuscarDuplicado(nombreNormalizado, sku)
    DAO->>DB: SELECT producto WHERE nombre_normalizado = ? OR sku = ?
    DB-->>DAO: Resultado
    DAO-->>Repo: Producto o null
    Repo-->>VM: Producto o null

    alt Existe y está activo
        VM-->>Screen: Error: producto duplicado
        Screen-->>Usuario: El producto ya existe
    else Existe pero está eliminado
        VM-->>Screen: Ofrecer reactivar
        Usuario->>Screen: Reactivar
        Screen->>VM: ReactivarProducto(idProducto)
        VM->>Repo: Reactivar(idProducto)
        Repo->>DAO: UPDATE producto SET activo = 1
        Screen-->>Usuario: Producto reactivado
    else No existe
        VM->>Repo: InsertarProducto(producto)
        Repo->>DAO: INSERT INTO producto
        DAO-->>Repo: id_producto generado
        VM-->>Screen: Actualizar estado
        Screen-->>Usuario: Producto agregado
        Note over Screen,Usuario: Se muestra en gris porque aún no tiene lotes
    end
```
</details>

<details>
<summary><b>Ingreso de lotes</b></summary>

```mermaid
sequenceDiagram
    actor Usuario
    participant View as AgregarLoteScreen
    participant VM as AgregarLoteViewModel
    participant Repo as LoteRepository
    participant DAO as LoteDAO
    participant DB as SQLite

    Usuario->>View: Ingresar precio de compra, unidades y vencimiento
    View->>VM: AgregarLote(idProducto, precioCompra, unidades, vencimiento)
    VM->>VM: Validar (unidades > 0, precio >= 0, vencimiento opcional)

    alt Datos inválidos
        VM-->>View: Error de validación
        View-->>Usuario: Revise los datos ingresados
    else Datos válidos
        VM->>Repo: AlmacenarLote(lote con fechaIngreso = hoy)
        Repo->>DAO: InsertarLote(lote)
        DAO->>DB: INSERT INTO lote
        DB-->>DAO: id_lote generado
        VM-->>View: Actualizar estado
        View-->>Usuario: Lote agregado
        Note over View,Usuario: El producto deja de verse gris porque ahora tiene stock
    end
```
</details>

<details>
<summary><b>Venta con escaneo y descuento FEFO</b></summary>

```mermaid
sequenceDiagram
    actor Usuario
    participant Scanner as EscanerCamara
    participant View as VentaScreen
    participant VM as VentaViewModel
    participant PRepo as ProductoRepository
    participant VRepo as VentaRepository
    participant DB as SQLite

    Usuario->>View: Iniciar venta
    View->>Scanner: Activar cámara
    loop Por cada producto
        alt Producto con código
            Scanner->>View: Código detectado
            View->>VM: EscanearSku(codigo)
            VM->>PRepo: BuscarPorSku(codigo)
        else Producto sin código
            Usuario->>View: Elegir de la lista
            View->>VM: AgregarProducto(idProducto)
        end
        PRepo-->>VM: Producto y stock o null
        alt No encontrado
            View-->>Usuario: Ofrecer registrar el producto
        else Sin stock
            View-->>Usuario: Mostrar aviso
        else Válido
            VM->>VM: Sumar a la boleta y recalcular total
            View-->>Usuario: Producto agregado (sonido o vibración)
        end
    end

    Usuario->>View: Aceptar venta
    View->>VM: ConfirmarVenta()
    VM->>VRepo: RegistrarVenta(boleta)
    VRepo->>DB: BEGIN
    VRepo->>DB: INSERT INTO venta
    loop Por cada línea
        VRepo->>DB: Lotes con stock ORDER BY vencimiento (FEFO)
        loop Hasta cubrir la cantidad
            VRepo->>DB: UPDATE lote SET unidades_disponibles = unidades_disponibles - n
            VRepo->>DB: INSERT INTO detalle_venta
        end
    end
    alt Todo correcto
        VRepo->>DB: COMMIT
        View-->>Usuario: Venta realizada
    else Stock insuficiente o error
        VRepo->>DB: ROLLBACK
        View-->>Usuario: No se pudo registrar la venta
    end
```
</details>

<details>
<summary><b>Merma</b></summary>

```mermaid
sequenceDiagram
    actor Usuario
    participant View as MermaScreen
    participant VM as MermaViewModel
    participant Repo as MermaRepository
    participant DB as SQLite

    View->>VM: CargarLotes(idProducto)
    VM-->>View: Lotes con stock (vencimiento y unidades)
    Usuario->>View: Elegir lote, cantidad, motivo y nota
    View->>VM: RegistrarMerma(idLote, cantidad, motivo, nota)
    VM->>VM: Validar (0 < cantidad <= disponibles)
    alt Datos inválidos
        View-->>Usuario: Revise los datos ingresados
    else Datos válidos
        VM->>Repo: RegistrarMerma(...)
        Repo->>DB: BEGIN
        Repo->>DB: UPDATE lote SET unidades_disponibles = unidades_disponibles - cantidad
        Repo->>DB: INSERT INTO perdida
        alt Todo correcto
            Repo->>DB: COMMIT
            View-->>Usuario: Merma registrada
        else Error
            Repo->>DB: ROLLBACK
            View-->>Usuario: No se pudo registrar la merma
        end
    end
```
</details>

## 🚀 Cómo compilar

**Requisitos:** Android Studio (o JDK 17 y Android SDK 37) y un teléfono o emulador con Android 7.0 o superior.

```bash
git clone https://github.com/CokkeDev/Mi-Negocio.git
cd Mi-Negocio
./gradlew assembleDebug      # APK de prueba en app/build/outputs/apk/debug/
./gradlew testDebugUnitTest  # Ejecuta los tests
```

<details>
<summary><b>Generar un APK release firmado</b></summary>

1. Crea tu llave de firma (guárdala bien: sin ella no podrás actualizar la app instalada):

   ```bash
   keytool -genkeypair -v -keystore mi-negocio.jks -alias minegocio -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Crea `keystore.properties` en la raíz del proyecto:

   ```properties
   storeFile=mi-negocio.jks
   storePassword=tu_contraseña
   keyAlias=minegocio
   keyPassword=tu_contraseña
   ```

3. Compila:

   ```bash
   ./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
   ```

`keystore.properties` y los archivos `.jks` están en `.gitignore` y **nunca deben subirse al repositorio**.
</details>

## 🧪 Tests

43 tests locales (se ejecutan en el computador con Robolectric, sin emulador):

| Área | Qué se verifica |
|---|---|
| Ventas | Descuento FEFO, total y ganancia por lote, rollback si falta stock |
| Merma y gastos | Descuento del lote, costo de la pérdida, listado y suma por categoría |
| Reportes | Ganancia neta, rangos de fechas, más vendidos, últimos 6 meses |
| Productos | Stock por lotes, próximo vencimiento, eliminar/reactivar, códigos únicos |
| Migraciones | Una base de datos de versiones anteriores (1 y 2) sube a la 3 sin perder datos |
| Respaldo | Solo acepta respaldos válidos de Mi Negocio |
| Utilidades | Reparto FEFO, formato de pesos, normalización de nombres |



<div align="center">

Hecho por [CokkeDev](https://github.com/CokkeDev)

</div>
