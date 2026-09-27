package com.example.barcodescanner

import java.io.File
import java.io.OutputStreamWriter

object CsvExporter {

    /**
     * Writes [records] to [file] as CSV with a header row.
     * Includes a UTF-8 BOM so Excel (Windows) correctly detects the encoding
     * instead of mangling non-ASCII characters (e.g. Persian/Arabic text in QR codes).
     */
    fun writeCsv(file: File, records: List<ScanRecord>) {
        file.outputStream().use { fos ->
            OutputStreamWriter(fos, Charsets.UTF_8).use { writer ->
                writer.write("\uFEFF")
                writer.append("Value,Format,Timestamp\n")
                for (record in records) {
                    writer.append(escape(record.value))
                    writer.append(',')
                    writer.append(escape(record.format))
                    writer.append(',')
                    writer.append(escape(record.timestamp))
                    writer.append('\n')
                }
            }
        }
    }

    private fun escape(field: String): String {
        val needsQuoting = field.contains(",") || field.contains("\"") || field.contains("\n")
        val escaped = field.replace("\"", "\"\"")
        return if (needsQuoting) "\"$escaped\"" else escaped
    }
}
