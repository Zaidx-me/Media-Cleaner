package com.zaidxme.whatsappcleaner.data

import android.content.Context
import android.text.format.Formatter.formatFileSize
import android.util.Log
import com.zaidxme.whatsappcleaner.Constants
import com.zaidxme.whatsappcleaner.model.ListDirectory
import com.zaidxme.whatsappcleaner.model.ListFile
import java.io.File

class FileRepository {
    companion object {
        private const val TRASH_SUFFIX = ".trash"

        @JvmStatic
        suspend fun getDirectoryList(
            context: Context,
            homePath: String,
        ): Pair<String, List<ListDirectory>> {
            Log.i("vishnu", "FileRepository#getDirectoryList: $homePath")

            val directoryList = ListDirectory.getDirectoryList(homePath).toMutableList()
            var totalSize = 0L

            directoryList.forEach { directoryItem ->
                val size = File(directoryItem.path).walkTopDown()
                    .filter { file -> file.isFile && file.name != ".nomedia" && !file.name.endsWith(TRASH_SUFFIX) }
                    .map { file -> file.length() }
                    .sum()

                directoryItem.size = formatFileSize(context, size)
                totalSize += size
            }

            return Pair(
                formatFileSize(context, totalSize),
                directoryList,
            )
        }

        @JvmStatic
        suspend fun getFileList(context: Context, path: String): ArrayList<ListFile> {
            Log.i("vishnu", "FileRepository#getFileList: $path")

            val list = ArrayList<ListFile>()

            if (path.contains("Media/WhatsApp Voice Notes") || path.contains("Media/WhatsApp Video Notes")) {
                File(path).walkTopDown().forEach { f ->
                    if (!f.isDirectory && f.name != ".nomedia" && !f.name.endsWith(TRASH_SUFFIX)) {
                        list.add(
                            ListFile(
                                f.path,
                                formatFileSize(context, getSize(f.path)),
                            ),
                        )
                    }
                }
            } else {
                File(path).listFiles { dir, name ->
                    val f = File("$dir/$name")

                    if (!f.isDirectory && f.name != ".nomedia" && !f.name.endsWith(TRASH_SUFFIX)) {
                        list.add(
                            ListFile(
                                f.path,
                                formatFileSize(context, getSize(f.path)),
                            ),
                        )
                    }

                    true
                }
            }

            return list
        }

        @JvmStatic
        suspend fun getTrashFileList(context: Context, path: String): ArrayList<ListFile> {
            Log.i("vishnu", "FileRepository#getTrashFileList: $path")

            val list = ArrayList<ListFile>()

            if (path.contains("Media/WhatsApp Voice Notes") || path.contains("Media/WhatsApp Video Notes")) {
                File(path).walkTopDown().forEach { f ->
                    if (!f.isDirectory && f.name.endsWith(TRASH_SUFFIX)) {
                        list.add(
                            ListFile(
                                f.path,
                                formatFileSize(context, getSize(f.path)),
                            ),
                        )
                    }
                }
            } else {
                File(path).listFiles { dir, name ->
                    val f = File("$dir/$name")

                    if (!f.isDirectory && f.name.endsWith(TRASH_SUFFIX)) {
                        list.add(
                            ListFile(
                                f.path,
                                formatFileSize(context, getSize(f.path)),
                            ),
                        )
                    }

                    true
                }
            }

            return list
        }

        @JvmStatic
        suspend fun getDirectoryList(path: String): ArrayList<String> {
            Log.i("vishnu", "FileRepository#getDirectoryList: $path")

            val list = ArrayList<String>()

            File(path).listFiles { dir, name ->
                val f = File("$dir/$name")

                if (f.isDirectory) list.add(f.path)

                true
            }

            return list
        }

        @JvmStatic
        fun getLoadingList(): ArrayList<ListFile> {
            val loadingList = ArrayList<ListFile>()

            for (i in 0 until 10) {
                loadingList.add(
                    ListFile(
                        Constants.LIST_LOADING_INDICATION,
                        "0 B",
                    ),
                )
            }

            return loadingList
        }

        @JvmStatic
        fun deleteFiles(fileList: List<ListFile>): Boolean {
            Log.i("vishnu", "FileRepository#deleteFiles: $fileList")

            fileList.forEach { file ->
                file.delete()
            }

            return false
        }

        @JvmStatic
        fun moveToTrashFiles(fileList: List<ListFile>): Boolean {
            Log.i("vishnu", "FileRepository#moveToTrashFiles: $fileList")

            fileList.forEach { file ->
                moveToTrash(file)
            }

            return true
        }

        @JvmStatic
        fun restoreFromTrashFiles(fileList: List<ListFile>): Boolean {
            Log.i("vishnu", "FileRepository#restoreFromTrashFiles: $fileList")

            fileList.forEach { file ->
                restoreFromTrash(file)
            }

            return true
        }

        private fun moveToTrash(file: ListFile): Boolean {
            val source = File(file.filePath)
            if (!source.exists()) return false
            if (source.name.endsWith(TRASH_SUFFIX)) return true

            var target = File(source.parentFile, source.name + TRASH_SUFFIX)
            var counter = 1

            while (target.exists()) {
                target = File(source.parentFile, "${source.name}.$counter$TRASH_SUFFIX")
                counter++
            }

            return source.renameTo(target)
        }

        private fun restoreFromTrash(file: ListFile): Boolean {
            val source = File(file.filePath)
            if (!source.exists()) return false
            if (!source.name.endsWith(TRASH_SUFFIX)) return false

            val restoredName = source.name.removeSuffix(TRASH_SUFFIX)
            var target = File(source.parentFile, restoredName)
            var counter = 1

            while (target.exists()) {
                target = File(source.parentFile, "$restoredName.restored$counter")
                counter++
            }

            return source.renameTo(target)
        }

        private fun getSize(path: String): Long {
            return File(path).walkTopDown().map { it.length() }.sum()
        }
    }
}
