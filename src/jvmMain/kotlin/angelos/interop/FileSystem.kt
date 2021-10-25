/**
 * Copyright (c) 2021 by Kristoffer Paulsson <kristoffer.paulsson@talenten.se>.
 *
 * This software is available under the terms of the MIT license. Parts are licensed
 * under different terms if stated. The legal terms are attached to the LICENSE file
 * and are made available on:
 *
 *      https://opensource.org/licenses/MIT
 *
 * SPDX-License-Identifier: MIT
 *
 * Contributors:
 *      Kristoffer Paulsson - initial implementation
 */
package angelos.interop

import angelos.io.FileSystem as RealFS
import angelos.io.FileNotFoundException
import angelos.io.IOException
import angelos.io.NotLinkException
import angelos.nio.Buffer
import angelos.nio.ByteDirectBuffer
import angelos.nio.ByteHeapBuffer

internal actual class FileSystem {
    actual companion object {
        actual inline fun readFile(number: Int, dst: Buffer, index: Int, count: Long): Long = when (dst) {
            is ByteHeapBuffer -> fs_read(number, dst.toArray(), index, count)
            is ByteDirectBuffer -> fs_pread(number, dst.toPtr(), index, count, dst.limit.toLong())
            else -> throw IOException("Unsupported buffer subtype.")
        }

        actual inline fun writeFile(number: Int, src: Buffer, index: Int, count: Long): Long = when (src) {
            is ByteHeapBuffer -> fs_write(number, src.toArray(), index, count)
            is ByteDirectBuffer -> fs_pwrite(number, src.toPtr(), index, count, src.limit.toLong())
            else -> throw IOException("Unsupported buffer subtype.")
        }

        actual inline fun tellFile(number: Int): Long = fs_lseek(number, 0, SeekDirective.CUR.whence)

        actual inline fun seekFile(number: Int, position: Long, whence: RealFS.Seek): Long =
            fs_lseek(
                number, position, when (whence) {
                    RealFS.Seek.SET -> SeekDirective.SET.whence
                    RealFS.Seek.CUR -> SeekDirective.CUR.whence
                    RealFS.Seek.END -> SeekDirective.END.whence
                }
            )

        actual inline fun closeFile(number: Int): Boolean = fs_close(number) == 0

        actual inline fun checkReadable(path: String): Boolean = fs_access(path, AccessFlag.R_OK.flag) == 0

        actual inline fun checkWritable(path: String): Boolean = fs_access(path, AccessFlag.W_OK.flag) == 0

        actual inline fun checkExecutable(path: String): Boolean = fs_access(path, AccessFlag.X_OK.flag) == 0

        actual inline fun checkExists(path: String): Boolean = fs_access(path, AccessFlag.F_OK.flag) == 0

        actual inline fun getFileType(path: String): Int {
            val type: Int = fs_filetype(path)
            if (type == -1)
                throw FileNotFoundException("File not found.\n$path")
            return type
        }

        actual inline fun getFileInfo(path: String): RealFS.Info =
            fs_fileinfo(path) ?: throw FileNotFoundException("File not found.\n$path")

        actual inline fun getLinkTarget(path: String): String =
            fs_readlink(path) ?: throw NotLinkException("Not a symbolic link.\n$path")

        actual inline fun openDir(path: String): Long {
            val number = fs_opendir(path)
            if (number == 0L)
                throw FileNotFoundException("Directory not found.\n$path")
            return number
        }

        actual inline fun readDir(dir: Long): RealFS.FileEntry = fs_readdir(dir)

        actual inline fun closeDir(dir: Long): Boolean = fs_closedir(dir) == 0

        actual inline fun openFile(path: String, option: Int): Int {
            val fd = fs_open(
                path, when (option) {
                    1 -> AccessMode.READ_ONLY.mode
                    2 -> AccessMode.WRITE_ONLY.mode
                    else -> AccessMode.READ_WRITE.mode
                }
            )
            if (fd == -1)
                throw FileNotFoundException("File not found.\n$path")
            return fd
        }

        enum class SeekDirective(val whence: Int) {
            SET(0),
            CUR(1),
            END(2),
        }

        enum class AccessFlag(val flag: Int) {
            F_OK(0),
            X_OK(0x01),
            W_OK(0x02),
            R_OK(0x04)
        }

        enum class AccessMode(val mode: Int) {
            READ_ONLY(0),
            WRITE_ONLY(0x01),
            READ_WRITE(0x02),
        }

        @JvmStatic
        private external fun fs_read(number: Int, array: ByteArray, index: Int, count: Long): Long

        @JvmStatic
        private external fun fs_pread(number: Int, ptr: Long, index: Int, count: Long, size: Long): Long

        @JvmStatic
        private external fun fs_write(number: Int, array: ByteArray, index: Int, count: Long): Long

        @JvmStatic
        private external fun fs_pwrite(number: Int, ptr: Long, index: Int, count: Long, size: Long): Long

        @JvmStatic
        private external fun fs_lseek(number: Int, offset: Long, whence: Int): Long

        @JvmStatic
        private external fun fs_close(number: Int): Int

        @JvmStatic
        private external fun fs_access(path: String, amode: Int): Int

        @JvmStatic
        private external fun fs_filetype(path: String): Int

        @JvmStatic
        private external fun fs_fileinfo(path: String): RealFS.Info?

        @JvmStatic
        private external fun fs_readlink(path: String): String?

        @JvmStatic
        private external fun fs_open(path: String, option: Int): Int

        @JvmStatic
        private external fun fs_opendir(path: String): Long

        @JvmStatic
        private external fun fs_readdir(dir: Long): RealFS.FileEntry

        @JvmStatic
        private external fun fs_closedir(dir: Long): Int

        init {
            System.loadLibrary("jnifilesystem")
        }
    }
}