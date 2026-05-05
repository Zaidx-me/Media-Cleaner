package com.zaidxme.whatsappcleaner

object Constants {
    const val WHATSAPP_HOME_URI = "whatsapp_home_uri"

    const val SCREEN_PERMISSION = "permission"
    const val SCREEN_HOME = "home"
    const val SCREEN_DETAILS = "details"

    const val DETAILS_LIST_ITEM = "details_list_item"
    const val FORCE_RELOAD_FILE_LIST = "force_reload_file_list"

    const val REQUEST_PERMISSIONS_CODE_WRITE_STORAGE = 2

    const val LIST_LOADING_INDICATION: String = "com.zaidxme.whatsappcleaner.loading"

    val EXTENSIONS_IMAGE = listOf(
        "jpg",
        "jpeg",
        "png",
        "webp",
        "gif",
        "bmp",
        "heic",
    )

    val EXTENSIONS_VIDEO = listOf(
        "mp4",
        "mkv",
        "3gp",
        "mov",
        "webm",
        "avi",
    )

    val EXTENSIONS_DOCS = listOf(
        "txt",
        "pdf",
        "doc",
        "odt",
        "ppt",
        "pptx",
        "odp",
        "xls",
        "xlsx",
        "ods",
    )

    val EXTENSIONS_AUDIO = listOf(
        "aac",
        "mp3",
        "flac",
        "opus",
        "midi",
        "wav",
        "ogg",
    )
}
