/*
 * Copyright 2019  Florian Spieß and the contributors of jda-reactor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package club.minnced.jda.reactor

import net.dv8tion.jda.api.entities.Icon
import net.dv8tion.jda.api.entities.Message
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.*
import java.nio.charset.Charset

/**
 * Retrieves the attachment as an InputStream
 *
 * @return[Mono]
 */
fun Message.Attachment.toInputStream(): Mono<InputStream> {
    return toByteArray()
            .map<InputStream> { ByteArrayInputStream(it) }
}

/**
 * Retrieves the attachment as an InputStream and converts it to a byte stream
 *
 * @return[Flux]
 */
fun Message.Attachment.toBytes(): Flux<Byte> {
    return toByteArray()
            .flatMapIterable { it.asIterable() }
}

/**
 * Retrieves the attachment as an InputStream and converts it to a stream of lines
 *
 * @return[Flux]
 */
fun Message.Attachment.toLines(): Flux<String> {
    return toInputStream()
            .map { InputStreamReader(it) }
            .flatMapMany {
                Flux.fromIterable(
                    it.buffered().lineSequence().asIterable()
                )
            }
}

/**
 * Retrieves the attachment as an InputStream and converts it to a ByteArray
 *
 * @return[Mono]
 */
fun Message.Attachment.toByteArray(): Mono<ByteArray> {
    return Mono.fromFuture { retrieveInputStream() }
               .map {
                   it.use { source ->
                       val buffer =  ByteArrayOutputStream()
                       source.copyTo(buffer)
                       buffer.toByteArray()
                   }
               }
               .subscribeOn(Schedulers.boundedElastic())
}

/**
 * Retrieves the attachment as an InputStream and converts it to its text content
 *
 * @param charset the [Charset] to use (default UTF-8)
 *
 * @return[Mono]
 */
fun Message.Attachment.toText(charset: Charset = Charsets.UTF_8): Mono<String> {
    return toByteArray()
            .map { it.toString(charset) }
}

/**
 * Retrieves the attachment as a file.
 *
 * - No parameters = Save in current working directory with the name provided by the attachment
 * - path parameter - Use the provided path (prefix with "/" to use absolute path)
 * - file parameter - Save to specific file object (useful for temporary files)
 *
 * @param path The file path, null to use current directory and name of attachment
 * @param file Target file, or current directory and name of attachment
 *
 * @return[Mono]
 */
fun Message.Attachment.toFile(path: String? = null, file: File? = null): Mono<File> {
    return Mono.fromFuture {
        when {
            path != null -> downloadToFile(path)
            file != null -> downloadToFile(file)
            else -> downloadToFile()
        }
    }.subscribeOn(Schedulers.boundedElastic())
}

/**
 * Retrieves the attachment as an InputStream and converts it to an Icon
 *
 * @return[Mono]
 */
fun Message.Attachment.toIcon(): Mono<Icon> {
    return toInputStream()
            .map { Icon.from(it) }
}

/**
 * Copies the attachment to the specified OutputStream and provides the number of copied bytes
 *
 * @param outputStream The target stream
 *
 * @return[Mono]
 */
fun Message.Attachment.copyTo(outputStream: OutputStream): Mono<Long> {
    return toInputStream()
            .map { it.copyTo(outputStream) }
}

