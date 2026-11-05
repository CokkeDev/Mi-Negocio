package cl.miproyecto.minegocio.data.mapper

// MAPPER = TRADUCTOR
// Este archivo convierte un producto de un "idioma" a otro:
//   ProductoEntity -> el producto como lo guarda la base de datos (Room).
//   Producto       -> el producto "limpio" que usa el resto de la app.
//
// Está en la capa "data" porque conoce a ProductoEntity (que es de Room).
// La capa "domain" nunca debe conocer a Room, por eso el traductor vive aquí.
//
// Este archivo no tiene "class": las funciones van sueltas en el archivo
// (se llaman "funciones de nivel superior").

// Se importan las dos clases que vamos a traducir.
// "data" puede importar cosas de "domain", pero nunca al revés.
import cl.miproyecto.minegocio.data.local.entity.ProductoEntity
import cl.miproyecto.minegocio.domain.model.Producto

// DE LA BASE DE DATOS -> A LA APP
// Se usa al LEER: cuando Room entrega un ProductoEntity, se convierte en Producto.
//
// "fun ProductoEntity.toDomain()" es una FUNCIÓN DE EXTENSIÓN:
// le "pega" la función toDomain() a ProductoEntity sin modificar esa clase.
// Se usa con un punto:  val producto = entity.toDomain()

//
// ": Producto" es lo que devuelve la función.
// "= Producto(...)": como la función solo crea y devuelve un objeto,
// se escribe con "=" en vez de { return ... }.
fun ProductoEntity.toDomain(): Producto = Producto(
    // Se lee: "el idProducto del Producto nuevo = el idProducto de esta entidad".
    // Izquierda: nombre del parámetro de Producto (argumento con nombre).
    // Derecha: la propiedad de la entidad sobre la que se llamó la función.
    // Usar nombres evita mezclar datos del mismo tipo (ej: nombre y nombreNormalizado).
    idProducto = idProducto,
    sku = sku,
    nombre = nombre,
    nombreNormalizado = nombreNormalizado,
    urlImagen = urlImagen,
    precioVenta = precioVenta,
    activo = activo,
)

// DE LA APP -> A LA BASE DE DATOS
// Se usa al GUARDAR: el Producto se convierte en ProductoEntity
// para entregárselo al DAO.  Ejemplo: dao.insertar(producto.toEntity())
//
// Es el camino de vuelta: ahora se le "pega" la función a Producto
// y devuelve un ProductoEntity.
fun Producto.toEntity(): ProductoEntity = ProductoEntity(
    // Mismas 7 propiedades, pero ahora se copian del Producto a la entidad.
    // Si el idProducto es 0 (producto nuevo), SQLite le asignará un número al insertarlo.
    idProducto = idProducto,
    sku = sku,
    nombre = nombre,
    nombreNormalizado = nombreNormalizado,
    urlImagen = urlImagen,
    precioVenta = precioVenta,
    activo = activo,
)
