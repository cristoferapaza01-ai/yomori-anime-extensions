# 🎬 Yomori Anime Extensions Repository

Repositorio independiente de extensiones de anime en **español** para aplicaciones estilo **Aniyomi / Mihon**.

---

## 📌 Extensiones Disponibles (`src/es/`)

| Fuente | Idioma | Estado | URL Base |
| :--- | :--- | :--- | :--- |
| **AnimeFLV** | 🇪🇸 Español | ✅ Activo | `https://animeflv.net` |
| **JKAnime** | 🇪🇸 Español | ✅ Activo | `https://jkanime.net` |
| **MonosChinos** | 🇪🇸 Español | ✅ Activo | `https://monoschinos2.com` |
| **TioAnime** | 🇪🇸 Español | ✅ Activo | `https://tioanime.com` |

---

## 🛠️ Estructura del Proyecto

```text
yomori-anime-extensions/
├── .github/
│   └── workflows/
│       └── build_push.yml    # CI/CD para compilar APKs y generar index.min.json
├── lib/
│   └── extractors/           # Extractores de vídeo compartidos (Sibnet, StreamTape, StreamWish, Voe)
├── src/
│   └── es/                   # Fuentes en español
│       ├── animeflv/
│       ├── jkanime/
│       ├── monoschinos/
│       └── tioanime/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🚀 Cómo Usar en tu App (Aniyomi / Yomi)

1. Sube este repositorio a tu cuenta de GitHub.
2. La GitHub Action `build_push.yml` compilará las extensiones automáticamente y creará la rama `repo` con el archivo `index.min.json`.
3. En la app (Aniyomi / Yomi), ve a **Ajustes** -> **Explorar** -> **Repositorios de extensiones**.
4. Añade tu URL del repositorio:
   ```text
   https://raw.githubusercontent.com/TU_USUARIO/yomori-anime-extensions/repo/index.min.json
   ```
