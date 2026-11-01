# Reglas de ProGuard/R8 específicas del proyecto.
# Se aplican cuando isMinifyEnabled = true en build.gradle.kts.
# Más información: https://developer.android.com/build/shrink-code

# Conservar números de línea en los stack traces de release.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
