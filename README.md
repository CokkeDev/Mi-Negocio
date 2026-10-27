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
    +Int Id_Producto PK
    +string sku UK 
    +String nombre
    +String nombre_normalizado
    +string URL_imagen
    +Int precio_venta
    +Int activo
  }

  class Lote {
    +Int id_lote PK
    +Int id_Producto FK
    +Int precio_compra
    +Date fecha_ingreso
    +Date Fecha_Vencimiento nullable
    +Int Unidades_Iniciales
    +Int unidades_Disponibles
  }

  class Perdida {
    +Int Id_perdida PK
    +Int Id_Lote FK
    +Int Cantidad
    +String Motivo
    +String Nota (opcional)
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
    +Int Id_Lote FK
    +Int Cantidad
    +Int Precio_Unitario
  }

  Producto "1" --> "*" Lote : tiene
  Lote "1" *-- "*" Perdida : Registra
  Lote "1" --> "*" Detalles_Venta : Se vende en
  Venta "1" --> "*" Detalles_Venta : Contiene
```

# Registro de producto (MVVM)

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
    VM->>VM: Validar datos
    VM->>VM: Normalizar nombre
    VM->>Repo: BuscarDuplicado(nombreNormalizado, sku)
    Repo->>DAO: BuscarDuplicado(nombreNormalizado, sku)
    DAO->>DB: SELECT producto WHERE nombre_normalizado = ? OR sku = ?
    DB-->>DAO: Resultado
    DAO-->>Repo: Producto o null
    Repo-->>VM: Producto o null

    alt Existe y está activo
        VM-->>Screen: Error: producto duplicado
        Screen-->>Usuario: No se pudo agregar, el producto ya existe
    else Existe pero está eliminado
        VM-->>Screen: Ofrecer reactivar
        Screen-->>Usuario: Desea reactivar el producto
        alt Usuario acepta
            Usuario->>Screen: Reactivar
            Screen->>VM: ReactivarProducto(idProducto)
            VM->>Repo: Reactivar(idProducto)
            Repo->>DAO: Reactivar(idProducto)
            DAO->>DB: UPDATE producto SET activo = 1
            DB-->>DAO: OK
            DAO-->>Repo: Confirmación
            Repo-->>VM: Producto reactivado
            VM-->>Screen: Actualizar estado
            Screen-->>Usuario: Producto reactivado exitosamente
        else Usuario cancela
            Screen-->>Usuario: Operación cancelada
        end
    else No existe
        VM->>Repo: InsertarProducto(producto)
        Repo->>DAO: InsertarProducto(producto)
        DAO->>DB: INSERT INTO producto
        DB-->>DAO: id_producto generado
        DAO-->>Repo: Confirmación
        Repo-->>VM: Producto creado
        VM-->>Screen: Actualizar estado
        Screen-->>Usuario: Producto agregado exitosamente
        Note over Screen,Usuario: Se muestra en gris porque aún no tiene lotes
    end
```

# Ingreso de lotes (MVVM)

```mermaid
sequenceDiagram
    actor Usuario
    participant View as HomeScreen
    participant VM as ProductoViewModel
    participant Repo as LoteRepository
    participant DAO as LoteDAO
    participant DB as SQLite

    Usuario->>View: SeleccionarProducto(idProducto o SKU)
    View->>VM: SeleccionarProducto(idProducto)
    VM-->>View: Mostrar información del producto

    Usuario->>View: Ingresar datos del lote
    View->>VM: AgregarLote(idProducto, PrecioCompra, Unidades, FechaVencimiento)

    VM->>VM: Validar datos del lote
    Note over VM: unidades mayor a 0, precio mayor o igual a 0, vencimiento opcional

    alt Datos inválidos
        VM-->>View: Error: datos inválidos
        View-->>Usuario: ERROR: revise los datos ingresados
    else Datos válidos
        VM->>VM: fechaIngreso = hoy
        VM->>VM: unidadesDisponibles = unidadesIniciales
        VM->>Repo: AlmacenarLote(lote)
        Repo->>DAO: InsertarLote(lote)
        DAO->>DB: INSERT Lote
        DB-->>DAO: id_lote generado
        DAO-->>Repo: Confirmación
        Repo-->>VM: Lote agregado exitosamente
        VM-->>View: Actualizar estado
        View-->>Usuario: Lote agregado exitosamente
        Note over View,Usuario: El producto deja de verse gris porque ahora tiene stock
    end
```
# Venta con escaneo de SKU (MVVM)

```mermaid
sequenceDiagram
    actor Usuario
    participant Scanner as EscanerCamara
    participant View as VentaScreen
    participant VM as VentaViewModel
    participant PRepo as ProductoRepository
    participant VRepo as VentaRepository
    participant DAO as ProductoDAO / VentaDAO
    participant DB as SQLite

    Usuario->>View: Iniciar venta
    View->>VM: IniciarVenta()
    VM->>VM: Crear boleta vacía en memoria
    View->>Scanner: Activar cámara

    loop Por cada producto
        alt Producto con código
            Usuario->>Scanner: Apuntar al código de barras
            Scanner->>View: Código detectado
            View->>VM: EscanearSku(codigo)
            VM->>PRepo: BuscarPorSku(codigo)
            PRepo->>DAO: SELECT producto y stock WHERE sku = ?
        else Producto sin código
            Usuario->>View: Tocar producto en el Home
            View->>VM: AgregarProducto(idProducto)
            VM->>PRepo: ObtenerConStock(idProducto)
            PRepo->>DAO: SELECT producto y stock WHERE id = ?
        end
        DAO->>DB: Consultar producto y SUM(unidades_disponibles)
        DB-->>DAO: Resultado
        DAO-->>PRepo: Producto y stock o null
        PRepo-->>VM: Producto y stock o null

        alt Producto no encontrado
            VM-->>View: Error: SKU no registrado
            View-->>Usuario: Ofrecer registrar el producto
        else Stock = 0
            VM-->>View: Error: sin stock
            View-->>Usuario: Mostrar mensaje
        else Producto válido
            alt Ya está en la boleta
                VM->>VM: Cantidad + 1 (máximo el stock disponible)
            else Nuevo en la boleta
                VM->>VM: Agregar línea con cantidad 1
            end
            VM->>VM: Recalcular total
            VM-->>View: Actualizar boleta
            View-->>Usuario: Producto agregado (vibración o sonido)
        end
    end

    alt Venta aceptada
        Usuario->>View: Aceptar boleta
        View->>VM: ConfirmarVenta()
        VM->>VRepo: RegistrarVenta(boleta)
        VRepo->>DAO: Iniciar transacción
        DAO->>DB: BEGIN
        DAO->>DB: INSERT INTO venta
        loop Por cada línea de la boleta
            DAO->>DB: SELECT lotes con stock ORDER BY fecha_vencimiento
            DB-->>DAO: Lotes disponibles
            loop Hasta cubrir la cantidad
                DAO->>DB: UPDATE lote SET unidades_disponibles = unidades_disponibles - n
                DAO->>DB: INSERT INTO detalle_venta (id_lote, cantidad, precio_unitario)
            end
        end

        alt Todo correcto
            DAO->>DB: COMMIT
            DAO-->>VRepo: Venta registrada
            VRepo-->>VM: Éxito
            VM->>VM: Limpiar boleta
            VM-->>View: Actualizar stock en Home
            View-->>Usuario: Venta realizada
        else Error (stock insuficiente o fallo de BD)
            DAO->>DB: ROLLBACK
            DAO-->>VRepo: Error
            VRepo-->>VM: Error
            VM-->>View: Error al registrar
            View-->>Usuario: No se pudo registrar la venta
        end
    else Venta rechazada
        Usuario->>View: Rechazar boleta
        View->>VM: CancelarVenta()
        VM->>VM: Descartar boleta
        VM-->>View: Volver al Home
        View-->>Usuario: Venta cancelada
    end
```

# Merma (MVVM)

```mermaid
sequenceDiagram
    actor Usuario
    participant View as MermaScreen
    participant VM as MermaViewModel
    participant LRepo as LoteRepository
    participant PRepo as PerdidaRepository
    participant DAO as LoteDAO / PerdidaDAO
    participant DB as SQLite

    Usuario->>View: Seleccionar producto
    View->>VM: CargarLotes(idProducto)
    VM->>LRepo: ObtenerLotesConStock(idProducto)
    LRepo->>DAO: ObtenerLotesConStock(idProducto)
    DAO->>DB: SELECT lotes WHERE unidades_disponibles mayor a 0
    DB-->>DAO: Lotes
    DAO-->>LRepo: Lotes
    LRepo-->>VM: Lotes
    VM-->>View: Mostrar lotes (vencimiento y unidades)

    Usuario->>View: Elegir lote, cantidad, motivo y nota
    View->>VM: RegistrarMerma(idLote, cantidad, motivo, nota)
    VM->>VM: Validar datos (cantidad mayor a 0 y menor o igual a las disponibles)

    alt Datos inválidos
        VM-->>View: Error de validación
        View-->>Usuario: ERROR: revise los datos ingresados
    else Datos válidos
        VM->>PRepo: RegistrarPerdida(perdida)
        PRepo->>DAO: RegistrarPerdida(perdida)
        DAO->>DB: BEGIN
        DAO->>DB: UPDATE lote SET unidades_disponibles = unidades_disponibles - cantidad
        DAO->>DB: INSERT INTO perdida (id_lote, cantidad, motivo, nota, fecha)

        alt Todo correcto
            DAO->>DB: COMMIT
            DAO-->>PRepo: Confirmación
            PRepo-->>VM: Merma registrada
            VM-->>View: Actualizar estado
            View-->>Usuario: Merma registrada exitosamente
        else Error (CHECK de stock o fallo de BD)
            DAO->>DB: ROLLBACK
            DAO-->>PRepo: Error
            PRepo-->>VM: Error
            VM-->>View: Error al registrar
            View-->>Usuario: No se pudo registrar la merma
        end
    end
```

