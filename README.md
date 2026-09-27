# Mi-Negocio
Crear un aplicación la cual pueda usarse de manera offline para mantener stock, ventas ganancias e historial del negocio de manera simple para el usuario.

# Stack tecnológico

- Kotlin lenguaje de desarrollo, este esta enfocado en Java pero para dispositivos Android.
- Sql lite para la base de datos.

# Arquitectura del proyecto

El proyecto utilizara la arquitectura de Modelo vista vista modelo para separar la interfaz de usuario de la lógica de negocio y de presentación.

las carpetas que tiene el proyecto esta enfocada de la siguiente manera:

- Core: funcionalidades comunes a toda la aplicación que no representan específicamente las reglas del negocio.
- Presentation: Aqui se agrega todo lo que va relacionado a la interfaz que el usuario visualizaria en la aplicación.
- Domain: Qué es lo que debe de hacer el negocio, lo que define como funciona.
- Data: cómo obtengo y guardo los datos.

# El MVP de la apliación

1.- Identificar el problema

2.- Seleccionar las funciones escenciales

No existe una aplicación simple donde se pueda mantener un registro del stock del kiosko y registrar las ganancias que se obtienen de manera que no tenga que registrar con una cuenta y que se mantenga todo de manera offline, las funciones que tiene la aplicación son:

Poder agregar productos que se venderan en el kiosko donde el usuario podra agregar, foto del producto, Precio de venta, Precio de compra, Nombre del producto, Fecha de vencimiento y stock disponible.

Eliminar productos, el usuario podra eliminar los porductos que agrego en caso de haber un error.

Vender producto, cuando seleciona la opción de vender se abrira una ventana donde se seleccionara la cantidad vendida y eso se le restara al stock, se le sumara la ganancias.

Merma, opción en caso de que el produto no se haya podido vender y se hecha a perder.

ver registros de ganacias, se mostrara los productos más vendidos, con las ganancias y los gastos realizados y los reportes de los meses con sus graficos representando los meses con mas ganancias y perdidas.

# Entindad relación BD
```mermaid
classDiagram
  class Producto {
    +Varchar imagen_producto
    +Int Id_Producto PK
    +String nombre
    +String nombre_normalizado
    +Int precio_venta
  }

  class Lote {
    +Int SKU
    +Int id_Producto
    +Int precio_compra
    +Date Fecha_Vencimiento
    +Int Unidades
  }

  class Perdida {
    +Int Id_perdida PK
    +Int Id_Lote FK
    +Int Cantidad
    +String Motivo
    +Date Fecha
  }

  class Venta {
    +Int Id_venta PK
    +Date Fecha
    +Int Total
  }

    class Detalles_Venta{
    +Int Id_detalle PK
    +Int Id_venta FK
    +Int Id_lote FK
    +Int Cantidad
    +Int Precio_Unitario
  }

  Producto "1" --> "*" Lote : Contiene
  Lote "1" *-- "*" Perdida : Registra
  Lote "1" --> "*" Detalles_Venta : Se vende
  Venta "1" --> "*" Detalles_Venta : Contiene
```


#  diagramas de secuencia

Usuario Compra mercadería y debe de registrar los productos en el sistema.
 
```mermaid
sequenceDiagram
    actor Usuario
    participant Home
    participant DataBase
    Usuario->>Home: AgregaProducto();
    Home->>DataBase: ExisteProducto();

    alt Producto Registrado
        DataBase-->>DataBase: Si existe
        DataBase-->>Home: ExisteProducto();
        Home-->>Usuario: No se pudo agregar, el producto ya existe.
    else Producto No Registrado
        DataBase-->>DataBase: No existe
        DataBase-->>Home: ExisteProducto();
        Home-->>Usuario: Producto agregado exitosamente.
    end
```

Cuando el usuario abre la aplicación, se encuentra en el Home. Para agregar un nuevo producto, debe seleccionar el botón "+", lo que abrirá la pantalla AgregarProducto().

En esta pantalla, el usuario deberá ingresar la imagen, nombre y precio de venta del producto. Una vez completada la información, la aplicación ejecutará la función ExisteProducto(), que consultará la base de datos para verificar si ya existe un producto con el mismo nombre.

Antes de realizar la comparación, el nombre ingresado será normalizado, considerando mayúsculas, minúsculas, acentos y caracteres especiales, con el objetivo de evitar registros duplicados por diferencias de escritura.

Si el producto ya existe: se informará al usuario que el producto ya se encuentra registrado y no se realizará la inserción.
Si el producto no existe: se almacenará el nuevo producto en la base de datos y se mostrará en el Home.

Como el producto recién creado no posee ningún lote asociado, se mostrará en color gris, indicando que actualmente no cuenta con unidades disponibles para la venta.

# Diagrama enfocado en la arquitectura MVVM del caso anterior

```mermaid
sequenceDiagram
    actor Usuario
    participant Screen as AgregarProductoScreen
    participant VM as ProductoViewModel
    participant Repo as ProductoRepository
    participant DAO as ProductoDAO
    participant DB as SQLite

    Usuario->>Screen: Completar datos del producto
    Usuario->>Screen: Presionar "Agregar"
    Screen->>VM: AgregarProducto(producto)
    VM->>VM: Normalizar nombre
    VM->>Repo: ExisteProducto(nombreNormalizado)
    Repo->>DAO: ExisteProducto(nombreNormalizado)
    DAO->>DB: Consultar producto

    alt Producto Registrado
        DB-->>DAO: Producto encontrado
        DAO-->>Repo: true
        Repo-->>VM: true
        VM-->>Screen: Producto ya existe
        Screen-->>Usuario: No se pudo agregar, el producto ya existe.
    else Producto No Registrado
        DB-->>DAO: Producto no encontrado
        DAO-->>Repo: false
        Repo-->>VM: false
        VM->>Repo: InsertarProducto(producto)
        Repo->>DAO: InsertarProducto(producto)
        DAO->>DB: Insertar producto
        DB-->>DAO: Producto guardado
        DAO-->>Repo: Confirmación
        Repo-->>VM: Producto creado
        VM-->>Screen: Actualizar pantalla
        Screen-->>Usuario: Producto agregado exitosamente.
    end
```
